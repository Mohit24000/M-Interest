package com.Minterest.ImageHosting.service;

import com.Minterest.ImageHosting.model.AppFeatures.EmailNotification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(EmailNotification notification) throws MessagingException {
        sendEmailWithImage(notification.getToEmail(), notification.getSubject(), notification.getBody());
    }

    public void sendCommentEmail(String toEmail, String content) throws MessagingException {
        // Keeping for backward compatibility or direct use if needed, 
        // but now it can also be used to build a notification object.

        String subject = "💬 Someone commented on your Pin!";

        String body = """
            <html>
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body style="margin:0; padding:0; background-color:#f0f2f5; font-family:'Segoe UI', Arial, sans-serif;">

              <table width="100%%%%" cellpadding="0" cellspacing="0" style="padding: 40px 0;">
                <tr>
                  <td align="center">

                    <!-- Card -->
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff; border-radius:16px;
                                  box-shadow:0 8px 30px rgba(0,0,0,0.12); overflow:hidden;">

                      <!-- Gradient Header -->
                      <tr>
                        <td align="center"
                            style="background: linear-gradient(135deg, #667eea 0%%%%, #764ba2 100%%%%);
                                   padding: 36px 24px 28px;">
                          <img src="cid:topImage" width="72" height="72"
                               style="border-radius:50%%; border: 3px solid rgba(255,255,255,0.4);
                                      margin-bottom:14px; display:block; margin-left:auto; margin-right:auto;" />
                          <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:700;
                                     letter-spacing:-0.3px;">MInterest</h1>
                          <p style="color:rgba(255,255,255,0.75); margin:4px 0 0; font-size:13px;">
                            Pin Activity Update
                          </p>
                        </td>
                      </tr>

                      <!-- Notification Icon Row -->
                      <tr>
                        <td align="center" style="padding: 28px 32px 0;">
                          <div style="background:#f0f4ff; border-radius:50%%;
                                      width:56px; height:56px; line-height:56px;
                                      font-size:28px; margin: 0 auto;">
                            💬
                          </div>
                          <h2 style="color:#1a1a2e; font-size:18px; font-weight:700;
                                     margin:14px 0 6px;">New Comment on Your Pin</h2>
                          <p style="color:#6b7280; font-size:14px; margin:0; line-height:1.6;">
                            Someone just left a comment on one of your pins. Here's what they said:
                          </p>
                        </td>
                      </tr>

                      <!-- Comment Content Box -->
                      <tr>
                        <td style="padding: 20px 32px 28px;">
                          <div style="background: linear-gradient(135deg, #f6f8ff 0%%%%, #f0f4ff 100%%%%);
                                      border-left: 4px solid #667eea;
                                      border-radius: 8px;
                                      padding: 16px 18px;">
                            <p style="color:#374151; font-size:15px; margin:0;
                                      line-height:1.65; font-style:italic;">
                              &ldquo;%s&rdquo;
                            </p>
                          </div>
                        </td>
                      </tr>

                      <!-- Divider -->
                      <tr>
                        <td style="padding: 0 32px;">
                          <hr style="border:none; border-top:1px solid #e5e7eb; margin:0;" />
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center" style="padding: 20px 32px 28px;">
                          <p style="color:#9ca3af; font-size:12px; margin:0; line-height:1.6;">
                            You received this email because you have notifications enabled on<br/>
                            <strong style="color:#667eea;">MInterest</strong> &nbsp;|&nbsp;
                            © 2026 MInterest App. All rights reserved.
                          </p>
                        </td>
                      </tr>

                    </table>
                    <!-- End Card -->

                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(content);

        sendEmailWithImage(toEmail, subject, body);
    }

    public void sendLikeEmail(String toEmail, String content) throws MessagingException {

        String subject = "❤️ Someone liked your Pin!";

        String body = """
            <html>
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body style="margin:0; padding:0; background-color:#f0f2f5; font-family:'Segoe UI', Arial, sans-serif;">

              <table width="100%%%%" cellpadding="0" cellspacing="0" style="padding: 40px 0;">
                <tr>
                  <td align="center">

                    <!-- Card -->
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff; border-radius:16px;
                                  box-shadow:0 8px 30px rgba(0,0,0,0.12); overflow:hidden;">

                      <!-- Gradient Header -->
                      <tr>
                        <td align="center"
                            style="background: linear-gradient(135deg, #f093fb 0%%%%, #f5576c 100%%%%);
                                   padding: 36px 24px 28px;">
                          <img src="cid:topImage" width="72" height="72"
                               style="border-radius:50%%; border: 3px solid rgba(255,255,255,0.4);
                                      margin-bottom:14px; display:block; margin-left:auto; margin-right:auto;" />
                          <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:700;
                                     letter-spacing:-0.3px;">MInterest</h1>
                          <p style="color:rgba(255,255,255,0.75); margin:4px 0 0; font-size:13px;">
                            Pin Activity Update
                          </p>
                        </td>
                      </tr>

                      <!-- Notification Icon Row -->
                      <tr>
                        <td align="center" style="padding: 28px 32px 0;">
                          <div style="background:#fff0f3; border-radius:50%%;
                                      width:56px; height:56px; line-height:56px;
                                      font-size:28px; margin: 0 auto;">
                            ❤️
                          </div>
                          <h2 style="color:#1a1a2e; font-size:18px; font-weight:700;
                                     margin:14px 0 6px;">Someone Liked Your Pin!</h2>
                          <p style="color:#6b7280; font-size:14px; margin:0; line-height:1.6;">
                            Your pin is spreading love! Someone just liked your content on MInterest.
                          </p>
                        </td>
                      </tr>

                      <!-- Like Message Box -->
                      <tr>
                        <td style="padding: 20px 32px 28px;">
                          <div style="background: linear-gradient(135deg, #fff5f7 0%%%%, #ffe4e8 100%%%%);
                                      border-left: 4px solid #f5576c;
                                      border-radius: 8px;
                                      padding: 16px 18px;">
                            <p style="color:#374151; font-size:15px; margin:0;
                                      line-height:1.65;">
                              %s
                            </p>
                          </div>
                        </td>
                      </tr>

                      <!-- Divider -->
                      <tr>
                        <td style="padding: 0 32px;">
                          <hr style="border:none; border-top:1px solid #e5e7eb; margin:0;" />
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center" style="padding: 20px 32px 28px;">
                          <p style="color:#9ca3af; font-size:12px; margin:0; line-height:1.6;">
                            You received this email because you have notifications enabled on<br/>
                            <strong style="color:#f5576c;">MInterest</strong> &nbsp;|&nbsp;
                            © 2026 MInterest App. All rights reserved.
                          </p>
                        </td>
                      </tr>

                    </table>
                    <!-- End Card -->

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

        ClassPathResource image = new ClassPathResource("static/email/anya.jpg");
        helper.addInline("topImage", image);

        mailSender.send(message);
    }
}
