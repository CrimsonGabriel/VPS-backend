package com.bazunia.vps.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

/**
 * Serwis odpowiedzialny za wysyłanie e-maili.
 * Używa JavaMailSender Springa.
 */
@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;

    // Pobieramy e-mail "od" z pliku properties, aby nie był na sztywno
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Wysyła e-mail aktywacyjny.
     * Uruchamiane asynchronicznie, aby nie blokować wątku rejestracji.
     * @param to Adres e-mail odbiorcy.
     * @param activationLink Pełny link aktywacyjny.
     */
    @Async
    public void sendActivationEmail(String to, String activationLink) {
        logger.info("Próba wysłania e-maila aktywacyjnego do: " + to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Aktywuj swoje konto w Bazunia App");

            String text = "Witaj,\n\n"
                    + "Dziękujemy za rejestrację. Kliknij poniższy link, aby aktywować swoje konto:\n\n"
                    + activationLink + "\n\n"
                    + "Jeśli to nie Ty zakładałeś konto, zignoruj tę wiadomość.\n\n"
                    + "Pozdrawiamy,\n"
                    + "Zespół Bazunia";

            message.setText(text);
            mailSender.send(message);

            logger.info("E-mail aktywacyjny pomyślnie wysłany do: " + to);
        } catch (MailException e) {
            logger.severe("BŁĄD wysyłania e-maila aktywacyjnego do " + to + ": " + e.getMessage());
        }
    }

//    /**
//     * Wysyła e-mail powitalny po aktywacji.
//     * @param to Adres e-mail odbiorcy.
//     */
//    @Async
//    public void sendWelcomeEmail(String to) {
//        logger.info("Próba wysłania e-maila powitalnego do: " + to);
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(fromEmail);
//            message.setTo(to);
//            message.setSubject("Witaj w Bazunia App!");
//
//            String text = """
//                    Witaj,
//
//                    Twoje konto zostało pomyślnie aktywowane!
//                    Możesz teraz zalogować się do aplikacji.
//
//                    Miłego korzystania!
//                    Zespół Bazunia""";
//
//            message.setText(text);
//            mailSender.send(message);
//
//            logger.info("E-mail powitalny pomyślnie wysłany do: " + to);
//        } catch (MailException e) {
//            logger.severe("BŁĄD wysyłania e-maila powitalnego do " + to + ": " + e.getMessage());
//        }
//    }
    /**
     * ⭐️⭐️ NOWA METODA ⭐️⭐️
     * Wysyła e-mail z linkiem do resetowania hasła.
     * @param to Adres e-mail odbiorcy.
     * @param resetLink Pełny link do zresetowania hasła.
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        logger.info("Próba wysłania e-maila resetującego hasło do: " + to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Resetowanie hasła w Bazunia App");

            String text = "Witaj,\n\n"
                    + "Otrzymaliśmy prośbę o zresetowanie hasła dla Twojego konta. Kliknij poniższy link, aby ustawić nowe hasło:\n\n"
                    + resetLink + "\n\n"
                    + "Link jest ważny przez 1 godzinę.\n\n"
                    + "Jeśli to nie Ty prosiłeś o reset, zignoruj tę wiadomość.\n\n"
                    + "Pozdrawiamy,\n"
                    + "Zespół Bazunia";

            message.setText(text);
            mailSender.send(message);

            logger.info("E-mail resetujący hasło pomyślnie wysłany do: " + to);
        } catch (MailException e) {
            logger.severe("BŁĄD wysyłania e-maila resetującego do " + to + ": " + e.getMessage());
        }
    }
}