package com.example.account.dto;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.type.TransactionResultType;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class CancelBalance {

    /**
     * {
     * "transactionId":"c2033bb6d82a4250aecf8e27c49b63f6",
     * "accountNumber":"1000000000",
     * "amount":1000
     * }
     */

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request implements AccountLockIdInterface {
        @NotBlank
        private String transactionId;

        @NotBlank
        @Size(min = 10, max = 10)
        private String accountNumber;

        @NotNull
        @Min(10)
        @Max(1000_000_000)
        private Long amount;
    }

    /**
     * {
     * "accountNumber":"1000000000",
     * "transactionResult":"S",
     * "transactionId":"5d011bb6d82cc50aecf8e27cdabb6772",
     * "amount":1000,
     * "transactedAt":"2022-06-01T23:26:14.671859"
     * }
     */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response{
        private String accountNumber;
        private TransactionResultType transactionResultType;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        public static Response from(TransactionDto useBalance) {
            return Response.builder()
                    .accountNumber(useBalance.getAccountNumber())
                    .transactionResultType(useBalance.getTransactionResultType())
                    .transactionId(useBalance.getTransactionId())
                    .amount(useBalance.getAmount())
                    .transactedAt(useBalance.getTransactedAt())
                    .build();
        }
    }
}
