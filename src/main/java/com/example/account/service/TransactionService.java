package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.*;
import static com.example.account.type.TransactionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 사용자 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
     * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 작거나 큰 경우 실패 응답
     *
     * @param userId : 유저 Id
     * @param accountNumber : 거래 중인 계좌 번호
     * @param amount : 거래량
     * @return : dto
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount){
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(amount, user, account);

        account.useBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(amount, account, S, USE)
        );
    }

    private static void validateUseBalance(Long amount, AccountUser user, Account account) {
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())){
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() != AccountStatus.IN_USE){
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount){
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransactions(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(amount, account, F, USE);
    }

    private Transaction saveAndGetTransaction(Long amount, Account account,
                                                 TransactionResultType resultType,
                                                 TransactionType transactionType) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(resultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateCancelBalance(amount, transaction, account);

        account.cancelBalance(amount);

       return TransactionDto.fromEntity(
               saveAndGetTransaction(amount, account, S, CANCEL)
       );
    }

    private static void validateCancelBalance(Long amount, Transaction transaction, Account account) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())){
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if (!Objects.equals(transaction.getAmount(), amount)){
            throw new AccountException(CANCEL_MUST_FULLY);
        }

        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))){
            throw new AccountException(TOO_OLD_TRANSACTION_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransactions(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(amount, account, F, CANCEL);
    }

    public TransactionDto queryTransaction(String transactionId) {
        return TransactionDto.fromEntity(
                transactionRepository.findByTransactionId(transactionId)
                        .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND))
        );
    }
}
