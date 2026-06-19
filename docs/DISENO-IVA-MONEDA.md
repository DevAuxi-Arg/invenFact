# Diseño — IVA por categoría y multimoneda (ARS / USD)

> Estado: **✅ implementado** (Fases 1–3). La facturación con snapshot (§10) queda pendiente.
> Nota de implementación: `alicuotaIva` y `moneda` se aceptan opcionales en los DTOs con
> default `21.00` / `ARS` (en vez de `@NotNull`), para no romper payloads existentes.
> Objetivo: que cada producto calcule su **precio final con IVA**, donde la **alícuota la determina la categoría**, el producto pueda estar valuado en **pesos o dólares**, y la **conversión + IVA se calculen al momento** de listar/cotizar — dejando el modelo preparado para una futura **facturación con valores congelados**.

---

## 1. Decisiones de diseño

| Tema | Decisión | Motivo |
|---|---|---|
| Alícuota de IVA | **Columna en `Categoria`** (`alicuotaIva`) | Es un atributo de la categoría; hay muchas, cada una con su tasa. La mantiene el admin desde el CRUD existente. |
| Valor del dólar | **Tabla de parámetros** (`parametros`, clave `DOLAR`) | Valor global y único que cambia seguido. No pertenece a una categoría ni a un producto. |
| Moneda del producto | **Enum `Moneda { ARS, USD }`** en `Producto` | El producto guarda su precio en su moneda original. |
| Moneda base | **ARS** | Todo se convierte a pesos para mostrar/IVA. |
| Cálculo | **`PriceCalculationService`** (no en la entidad) | El cálculo necesita el dólar (parámetro externo); no puede salir limpio de un método de entidad. |
| Precio final | **Calculado al leer**, nunca persistido | Si cambia el dólar o el IVA, los listados reflejan el valor vigente. |
| Facturación (futuro) | **Snapshot**: la factura copia tipo de cambio, alícuota y totales | Una factura es un documento histórico: no debe cambiar si después se mueve el dólar o el IVA. |
| Tipo de dato monetario | **`BigDecimal`** con redondeo `HALF_UP` | Nunca `double` para dinero. |

Lo que **se descarta** del sistema anterior (`D:\willy\sistema`): herencia `ArticuloElectronico/Alimenticio/General`, IVA hardcodeado por subclase y uso de `double`.

---

## 2. Modelo de datos

### 2.1 `Categoria` (modificada)

Se agrega:

```java
@Column(nullable = false, precision = 5, scale = 2)
private BigDecimal alicuotaIva;   // porcentaje: 21.00, 10.50, 0.00 (exento)
```

- Rango válido: `0.00` … `100.00`.
- `0.00` = exento.

### 2.2 `Producto` (modificada)

Se agrega:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 3)
private Moneda moneda;            // ARS | USD
```

`precio` (ya existe, `precision = 12, scale = 2`) se interpreta **en la moneda del producto**.

### 2.3 `Moneda` (nuevo enum)

```java
public enum Moneda { ARS, USD }
```

### 2.4 `Parametro` (nueva entidad)

Tabla genérica clave/valor que mantiene el admin (extensible a futuros parámetros: datos de empresa para factura, IVA por defecto, etc.).

```java
@Entity @Table(name = "parametros")
class Parametro {
    @Id String clave;                 // "DOLAR"
    String valor;                     // "1000.00"  (texto; el service lo tipa)
    String descripcion;               // "Cotización del dólar en ARS"
    LocalDateTime fechaActualizacion;
    String actualizadoPor;
}
```

> **Opcional (extensión):** una tabla `tipos_cambio (id, valor, vigenciaDesde, cargadoPor)` para tener **historial** del dólar (útil para auditar y para snapshots de factura). El "dólar vigente" sería el registro más reciente. Para v1 alcanza con `Parametro`.

---

## 3. Fórmulas de cálculo

Moneda base = **ARS**. Sea:

- `precio` = precio del producto en su moneda
- `dolar` = parámetro `DOLAR` (ARS por USD)
- `alicuota` = `categoria.alicuotaIva` (porcentaje)

```
# 1) Neto en pesos
neto = (moneda == USD) ? precio × dolar : precio
neto = neto.setScale(2, HALF_UP)

# 2) IVA
ivaMonto = neto × (alicuota / 100)
ivaMonto = ivaMonto.setScale(2, HALF_UP)

# 3) Precio final
precioFinal = neto + ivaMonto
```

**Reglas de redondeo:** trabajar la multiplicación a escala completa y **redondear a 2 decimales (`HALF_UP`) en cada monto en pesos**. `dolar` se guarda con escala 4 para no perder precisión en la cotización.

**Ejemplo:** producto USD 100,00 · dólar 1.000,0000 · categoría "Electrónica" 10,50 %

```
neto        = 100,00 × 1000 = 100.000,00
ivaMonto    = 100.000,00 × 0,105 = 10.500,00
precioFinal = 110.500,00 ARS
```

---

## 4. Servicio de cálculo

```java
record PriceBreakdown(
    Moneda     monedaOriginal,
    BigDecimal precioOriginal,      // en la moneda del producto
    BigDecimal tipoCambioAplicado,  // DOLAR vigente (siempre presente: hace falta para la vista en USD)
    BigDecimal netoArs,             // convertido a pesos, sin IVA
    BigDecimal alicuotaIva,         // %
    BigDecimal ivaMontoArs,
    BigDecimal precioFinalArs,      // precio final en pesos
    BigDecimal precioFinalUsd       // mismo precio final, expresado en dólares
) {}

interface PriceCalculationService {
    PriceBreakdown calcular(Product producto);   // usa el DOLAR vigente
}
```

**Se muestran los dos precios** (decisión confirmada). La fuente de verdad es **ARS**; el valor en USD se deriva:

```
precioFinalUsd = precioFinalArs / dolar     (setScale(2, HALF_UP))
```

Así, sea el producto en ARS o en USD, siempre se presentan ambos importes de forma consistente.

Un único punto de cálculo, reutilizado por la API, las vistas y (a futuro) la facturación.

---

## 5. Cambios en la API REST

### 5.1 Parámetros — `/api/parametros`

| Método | Path | Descripción | Rol |
|---|---|---|---|
| GET | `/api/parametros` | Listar parámetros | ADMIN · CO-ADMIN |
| GET | `/api/parametros/{clave}` | Obtener uno (ej. `DOLAR`) | ADMIN · CO-ADMIN |
| PUT | `/api/parametros/{clave}` | Actualizar valor | **solo ADMIN** |

### 5.2 Categorías (modificado)

`CategoryRequest` / `CategoryResponse` suman `alicuotaIva` (validada `0..100`).

### 5.3 Productos (modificado)

- `ProductRequest`: suma `moneda` (`@NotNull`).
- `ProductResponse`: suma `moneda` y el **desglose** (`precioFinalArs`, **`precioFinalUsd`**, `ivaMontoArs`, `netoArs`, `tipoCambioAplicado`).
- Nuevo: `GET /api/productos/{id}/precio` → `PriceBreakdown` completo.

---

## 6. Cambios en la UI (Thymeleaf)

- **Form de categoría**: campo "Alícuota IVA (%)".
- **Form de producto**: selector de moneda (ARS/USD).
- **Listado de productos**: columnas "Precio final (ARS)" y "Precio final (USD)" calculadas en vivo, con tooltip del desglose.
- **Nueva página `/admin/parametros`** (solo ADMIN): editar el valor del dólar.
- **Dashboard**: tarjeta con el dólar vigente.

---

## 7. Roles y permisos

| Acción | ADMIN | CO-ADMIN | BACKOFFICE |
|---|:---:|:---:|:---:|
| Editar alícuota IVA (al gestionar categoría) | ✅ | ✅ | ❌ |
| Ver parámetros (dólar) | ✅ | ✅ | ❌ |
| **Editar el dólar** | ✅ | ❌ | ❌ |

(Coherente con la matriz existente: "Configuración del sistema → solo ADMIN").

---

## 8. Validaciones

- `alicuotaIva`: `@NotNull`, `@DecimalMin("0.00")`, `@DecimalMax("100.00")`, `@Digits(3,2)`.
- `moneda`: `@NotNull`.
- `Parametro DOLAR`: al actualizar, valor `> 0`, numérico, escala ≤ 4.

---

## 9. Migración y compatibilidad con datos existentes

Con `ddl-auto=update` Hibernate agrega las columnas, pero **columnas `NOT NULL` en tablas con filas existentes fallan sin default**. Plan seguro:

1. Agregar `categorias.alicuota_iva` y `productos.moneda` **con default a nivel BD** (`DEFAULT 21.00` y `DEFAULT 'ARS'`) o como nullable.
2. Backfill de filas existentes (`UPDATE` para fijar IVA y moneda).
3. Sembrar el parámetro `DOLAR` con un valor inicial (ej. `1.00`) para que el admin lo actualice.
4. (Si se agregaron nullable) recién entonces marcarlas `NOT NULL`.

> Recomiendo un script SQL de migración versionado (o, a futuro, **Flyway**) en vez de depender solo de `ddl-auto=update`.

---

## 10. Facturación con snapshot — ✅ implementada

> Implementada con "cliente mínimo" (nombre/documento como texto en la factura, sin entidad `Cliente`).
> Entidades `Factura` + `LineaFactura`; al emitir se **congelan** precio, tipo de cambio, alícuota y
> totales, se descuenta stock, y `anular` lo restituye. API en `/api/facturas`, UI en `/facturas`
> (listado, alta con líneas dinámicas, detalle imprimible). La condición "exento" llegará con la API de clientes.

La `LineaFactura` **congela** los valores del cálculo:

```
LineaFactura: productoId, nombre, monedaOriginal, precioOriginal,
              tipoCambioAplicado, alicuotaIva, netoArs, ivaMontoArs, totalArs
Factura:      fecha, cliente, subtotal, totalIva, total, ...
```

Así, el total histórico de una factura **no cambia** aunque después se muevan el dólar o las alícuotas. El `PriceCalculationService` produce el desglose; la facturación lo persiste copiado.

---

## 11. Plan de implementación por fases

1. **Parámetros**: entidad `Parametro` + `ParametroService` (getters tipados) + endpoints + página `/admin/parametros` + seed `DOLAR`.
2. **IVA por categoría**: campo `alicuotaIva` + validación + DTOs + form + migración/backfill.
3. **Moneda + cálculo**: enum `Moneda` + campo en producto + `PriceCalculationService` + desglose en API y listados.
4. *(futuro)* **Facturación** con snapshot.

---

## 12. Tests propuestos

- `PriceCalculationServiceTest`: ARS con IVA 21 %, USD con conversión, exento (0 %), redondeo `HALF_UP`.
- `ParametroServiceTest`: lectura tipada del dólar, actualización restringida a ADMIN.
- Validación de `alicuotaIva` fuera de rango → 400.

---

## 13. Decisiones resueltas

1. **Override de IVA por producto** → **NO**. El IVA se determina **solo por categoría**.
2. **Historial de dólar (`tipos_cambio`)** → **No en v1**. Alcanza con el parámetro simple `DOLAR`.
3. **Mostrar precio final en ARS y USD** → **Sí, los dos** (ver §4: `precioFinalArs` + `precioFinalUsd`).
4. **Condición de IVA / exento** → con el **porcentaje libre `0..100` alcanza** por ahora. El caso "exento" depende del **cliente**, así que se abordará cuando se construya la **API de clientes** (posible `CondicionIva` a futuro).
