package com.willysoft.productosapi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía correos si hay SMTP configurado y {@code app.mail.enabled=true}.
 * En caso contrario (desarrollo) registra el contenido en el log como respaldo,
 * de modo que el flujo de recuperación se puede probar sin servidor de correo.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.enabled}") boolean mailEnabled,
                        @Value("${app.mail.from}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || sender == null) {
            log.warn("""
                    [EMAIL deshabilitado] No se envió correo real.
                      Para: {}
                      Asunto: {}
                      Cuerpo:
                    {}""", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("Correo enviado a {}", to);
    }
}
