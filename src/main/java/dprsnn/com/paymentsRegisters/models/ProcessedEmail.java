package dprsnn.com.paymentsRegisters.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "processed_emails")
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "sender")
    private String sender;

    @Column(name = "sent_date")
    private Instant sentDate;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "attachment_path")
    private String attachmentPath;

    @Column(name = "has_attachments", nullable = false)
    private boolean hasAttachments;

    // Конструктори, гетери та сетери
    public ProcessedEmail() {
    }

    public ProcessedEmail(String messageId, String subject, String sender,
                          Instant sentDate, Instant processedAt,
                          String attachmentPath, boolean hasAttachments) {
        this.messageId = messageId;
        this.subject = subject;
        this.sender = sender;
        this.sentDate = sentDate;
        this.processedAt = processedAt;
        this.attachmentPath = attachmentPath;
        this.hasAttachments = hasAttachments;
    }

    // Гетери та сетери для всіх полів
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Instant getSentDate() {
        return sentDate;
    }

    public void setSentDate(Instant sentDate) {
        this.sentDate = sentDate;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public boolean isHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedEmail that = (ProcessedEmail) o;
        return hasAttachments == that.hasAttachments &&
                Objects.equals(id, that.id) &&
                Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, messageId, hasAttachments);
    }

    @Override
    public String toString() {
        return "ProcessedEmail{" +
                "id=" + id +
                ", messageId='" + messageId + '\'' +
                ", subject='" + subject + '\'' +
                ", sender='" + sender + '\'' +
                ", sentDate=" + sentDate +
                ", processedAt=" + processedAt +
                ", attachmentPath='" + attachmentPath + '\'' +
                ", hasAttachments=" + hasAttachments +
                '}';
    }
}