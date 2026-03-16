package com.Minterest.ImageHosting.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCommentEmail(String toEmail, String content) throws MessagingException {

        String subject = "So one Commment on your Pin";

        String body = """
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8;">
                <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                        <td align="center">
                            <table width="420" style="background:#ffffff; border-radius:12px;
                                box-shadow:0 4px 12px rgba(0,0,0,0.1); padding:20px;">
          
                                <!-- Image -->
                                <tr>
                                    <td align="center">
                                        <img src="cid:topImage" width="160" style="margin-bottom:15px;" />
                                    </td>
                                </tr>

                                <!-- Title -->
                                <tr>
                                    <td align="center">
                                        <h2 style="color:#2c3e50; margin:10px 0;">
                                           A message from MInterest
                                        </h2>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="color:#555; font-size:14px; text-align:center;">
                                        <p>Some one Comment on your pin </p>

                                        <div style="
                                            font-size:26px;
                                            font-weight:bold;
                                            letter-spacing:4px;
                                            color:#1abc9c;
                                            margin:15px 0;">
                                            %s
                                        </div>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="font-size:12px; color:#999; text-align:center; padding-top:15px;">
                                        © 2026 MInterest App. Secure & Trusted.
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(content);

        sendEmailWithImage(toEmail, subject, body);
    }

    public void sendLikeEmail(String toEmail, String content) throws MessagingException {

        String subject = "So one Comment on your Pin";

        String body = """
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8;">
                <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                        <td align="center">
                            <table width="420" style="background:#ffffff; border-radius:12px;
                                box-shadow:0 4px 12px rgba(0,0,0,0.1); padding:20px;">
          
                                <!-- Image -->
                                <tr>
                                    <td align="center">
                                        <img src="cid:topImage" width="160" style="margin-bottom:15px;" />
                                    </td>
                                </tr>

                                <!-- Title -->
                                <tr>
                                    <td align="center">
                                        <h2 style="color:#2c3e50; margin:10px 0;">
                                           A message from MInterest
                                        </h2>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="color:#555; font-size:14px; text-align:center;">
                                        <p>Some one Like on your pin </p>

                                        <div style="
                                            font-size:26px;
                                            font-weight:bold;
                                            letter-spacing:4px;
                                            color:#1abc9c;
                                            margin:15px 0;">
                                            %s
                                        </div>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="font-size:12px; color:#999; text-align:center; padding-top:15px;">
                                        © 2026 MInterest App. Secure & Trusted.
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(content);

        sendEmailWithImage(toEmail, subject, body);
    }

    private void sendEmailWithImage(String toEmail, String subject, String body)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true);


        ClassPathResource image = new ClassPathResource("static/email/anya.png");
        helper.addInline("topImage", image);

        mailSender.send(message);
    }
}

