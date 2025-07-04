package dprsnn.com.paymentsRegisters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRecord {
    private String orderId;
    private String sourceOrderId;
    private String typeOfPayment;
    private String paymentDate;
    private String amount;
    private String expense;
    private String ttn;

    public String getTtn() {
        return ttn;
    }

    public void setTtn(String ttn) {
        this.ttn = ttn;
    }

    public String getFormatedDate() {
        // Формат вхідного рядка: 08.06.2025
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        // Бажаний формат: 2025-06-08 HH:mm:ss
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDate date = LocalDate.parse(this.paymentDate, inputFormatter);
        LocalTime currentTime = LocalTime.now();

        return date.atTime(currentTime).format(outputFormatter);
    }

    public String getFormatedDatePlusOneDay() {
        // Формат вхідного рядка: 08.06.2025
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        // Бажаний формат: 2025-06-08 HH:mm:ss
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Парсимо дату
        LocalDate date = LocalDate.parse(this.paymentDate, inputFormatter);

        // Додаємо один день
        date = date.plusDays(1);

        // Поточний час
        LocalTime currentTime = LocalTime.now();

        // Повертаємо у потрібному форматі
        return date.atTime(currentTime).format(outputFormatter);
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(String sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public String getTypeOfPayment() {
        return typeOfPayment;
    }

    public void setTypeOfPayment(String typeOfPayment) {
        this.typeOfPayment = typeOfPayment;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getExpense() {
        return expense;
    }

    public void setExpense(String expense) {
        this.expense = expense;
    }

    @Override
    public String toString() {
        return "PaymentRecord{" +
                "orderId='" + orderId + '\'' +
                ", sourceOrderId='" + sourceOrderId + '\'' +
                ", typeOfPayment='" + typeOfPayment + '\'' +
                ", paymentDate='" + paymentDate + '\'' +
                ", amount='" + amount + '\'' +
                ", expense='" + expense + '\'' +
                '}';
    }
}