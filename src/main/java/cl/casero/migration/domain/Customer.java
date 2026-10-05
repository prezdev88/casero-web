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

import java.time.LocalDate;
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

    public boolean isBirthdayOn(LocalDate date) {
        return birthDay != null && birthMonth != null
                && birthDay == date.getDayOfMonth()
                && birthMonth == date.getMonthValue();
    }

    public Integer getBirthdayAgeOn(LocalDate date) {
        if (birthYear == null || !isBirthdayOn(date)) {
            return null;
        }
        return date.getYear() - birthYear;
    }
}
