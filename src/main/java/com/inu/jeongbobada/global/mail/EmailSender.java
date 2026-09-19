package com.inu.jeongbobada.global.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// JavaMailSender를 직접 주입하지 않고 ObjectProvider로 받는다.
// spring.mail.host가 없으면 JavaMailSender 빈 자체가 안 만들어지는데, 그래도 앱(과 CI 테스트)은 떠야 하기 때문.
// 이 경우 기동은 되고, 메일을 실제로 보내는 시점에만 예외가 난다.
@Component
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;

    public EmailSender(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${spring.mail.username:}") String from
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
    }

    public void send(String to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("메일 설정(spring.mail.*)이 없어 메일을 보낼 수 없습니다.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
