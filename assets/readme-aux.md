![Diagrama de datos](./der-color.png)


```
┌────────────────────────────────────────────────────────────┐
│  Presentación                                              │
│  ┌──────────────────────────┐  ┌────────────────────────┐  │
│  │  Web Controller          │  │  REST Controller       │  │
│  │  (@Controller)           │  │  (@RestController)     │  │
│  │  devuelve HTML           │  │  devuelve JSON         │  │
│  └──────────────────────────┘  └────────────────────────┘  │
├────────────────────────────────────────────────────────────┤
│  Negocio                                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Service (@Service)                                  │  │
│  │  lógica, validaciones de dominio, transacciones      │  │
│  └──────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────┤
│  Persistencia                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Repository (extends JpaRepository)                  │  │
│  │  consultas a la base de datos                        │  │
│  └──────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────┤
│  Datos                                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Entity (@Entity)                                    │  │
│  │  mapeo objeto–tabla (filas de PostgreSQL)            │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

```mermaid
graph TD
    subgraph Presentacion ["capa de Presentación"]
        WC["Web Controller<br/>(@Controller)<br/><sub>devuelve HTML</sub>"]
        RC["REST Controller<br/>(@RestController)<br/><sub>devuelve JSON</sub>"]
    end

    subgraph Negocio ["capa de Negocio"]
        S["Service (@Service)<br/><sub>lógica, validaciones, transacciones</sub>"]
    end

    subgraph Persistencia ["capa de Persistencia"]
        R["Repository (extends JpaRepository)<br/><sub>consultas a DB</sub>"]
    end

    subgraph Datos ["capa de Datos"]
        E["Entity (@Entity)<br/><sub>mapeo objeto-tabla</sub>"]
    end

    WC --> S
    RC --> S
    S --> R
    R --> E

    style Presentacion fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Negocio fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style Persistencia fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style Datos fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
```

![Diagrama de datos](./der-color.png)


```mermaid
erDiagram
  CATEGORIAS {
    int id PK "id (PK)"
    string nombre "nombre (unique)"
    string descripcion
  }

  PRODUCTOS {
    int id PK "id (PK)"
    string nombre
    string descripcion
    decimal precio
    int stock
    int categoria_id FK
  }

  CATEGORIAS ||--o{ PRODUCTOS : "tiene"
```


---

## Estructura del proyecto

```
src/main/
├── java/com/willysoft/productosapi/
│   ├── ProductosApiApplication.java
│   ├── config/
│   │   └── OpenApiConfig.java
│   ├── category/                            ← feature: categorías
│   │   ├── Category.java                    [Entity = Modelo]
│   │   ├── CategoryRepository.java          [Persistencia]
│   │   ├── CategoryService.java             [Negocio]
│   │   ├── CategoryController.java          [REST: /api/categorias]
│   │   └── dto/
│   │       ├── CategoryRequest.java
│   │       └── CategoryResponse.java
│   ├── product/                             ← feature: productos
│   │   ├── Product.java
│   │   ├── ProductRepository.java
│   │   ├── ProductService.java
│   │   ├── ProductController.java           [REST: /api/productos]
│   │   └── dto/
│   │       ├── ProductRequest.java
│   │       └── ProductResponse.java
│   ├── web/                                 ← capa MVC (vistas HTML)
│   │   ├── HomeController.java              [GET /]
│   │   ├── CategoryWebController.java       [/categorias/**]
│   │   └── ProductWebController.java        [/productos/**]
│   └── exception/
│       ├── ResourceNotFoundException.java   → 404
│       ├── ConflictException.java           → 409
│       └── GlobalExceptionHandler.java      → JSON uniforme (solo REST)
└── resources/
    ├── application.properties
    └── templates/                           ← Thymeleaf
        ├── layout.html                      [layout base con Bootstrap]
        ├── home.html
        ├── categorias/
        │   ├── list.html
        │   └── form.html
        └── productos/
            ├── list.html
            └── form.html
```

---