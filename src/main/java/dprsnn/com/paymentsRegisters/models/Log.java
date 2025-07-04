package dprsnn.com.paymentsRegisters.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String logText;

    private LocalDateTime logTime;

    public Log(String logText, LocalDateTime logTime) {
        this.logText = logText;
        this.logTime = logTime;
    }

    public Log() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogText() {
        return logText;
    }

    public void setLogText(String logText) {
        this.logText = logText;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }
}
