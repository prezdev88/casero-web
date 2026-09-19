package cl.casero.migration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Integer debt = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "customer")
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "birth_day")
    private Integer birthDay;

    @Column(name = "birth_month")
    private Integer birthMonth;

    @Column(name = "birth_year")
    private Integer birthYear;

    public String getFormattedBirthDate() {
        if (birthDay == null || birthMonth == null) {
            return null;
        }
        
        String[] months = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        
        String monthName = (birthMonth >= 1 && birthMonth <= 12) ? months[birthMonth - 1] : "";
        String base = birthDay + " de " + monthName;
        
        if (birthYear != null) {
            int age;
            try {
                java.time.LocalDate birthDate = java.time.LocalDate.of(birthYear, birthMonth, birthDay);
                age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
            } catch (Exception e) {
                age = java.time.LocalDate.now().getYear() - birthYear;
                if (java.time.LocalDate.now().getMonthValue() < birthMonth) {
                    age--;
                }
            }
            return base + " de " + birthYear + " (" + age + " años)";
        }
        return base;
    }
}
