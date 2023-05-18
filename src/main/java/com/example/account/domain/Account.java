package com.example.account.domain;


import com.example.account.exception.AccountException;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

import static com.example.account.type.ErrorCode.*;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Account extends BaseEntity{
    @ManyToOne
    private AccountUser accountUser;
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public void useBalance(Long amount){
        validateBalance(amount);
        balance -= amount;
    }

    public void cancelBalance(Long amount){
        validateAmount(amount);
        balance += amount;
    }

    private void validateAmount(Long amount) {
        if (amount < 0){
            throw new AccountException(INVALID_REQUEST);
        }
    }

    private void validateBalance(Long amount) {
        if (amount > balance){
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }
}
