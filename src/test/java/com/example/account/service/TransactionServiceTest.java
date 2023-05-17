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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.*;
import static com.example.account.type.TransactionType.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final long CANCEL_AMOUNT = 1000L;

    @Test
    @DisplayName("잔액 사용 성공")
    void UseBalanceSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account)
                );
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(
                12L, "1000000000", 1000L);

        //then
        verify(transactionRepository, times(1))
                .save(transactionArgumentCaptor.capture());


        assertThat(transactionArgumentCaptor.getValue().getBalanceSnapshot()).isEqualTo(9000L);
        assertThat(transactionArgumentCaptor.getValue().getTransactionType()).isEqualTo(USE);
        assertThat(transactionArgumentCaptor.getValue().getTransactionResultType()).isEqualTo(S);
        assertThat(transactionArgumentCaptor.getValue().getAmount()).isEqualTo(1000L);
        assertThat(transactionDto.getAccountNumber()).isEqualTo("1000000000");
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void createAccount_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void deleteAccount_AccountNotFound(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 다름")
    void deleteAccount_userUnMatch(){
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        AccountUser haru = AccountUser.builder()
                .id(13L)
                .name("Haru")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(haru)
                        .balance(0L)
                        .accountNumber("1000000000")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_ACCOUNT_UN_MATCH);
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 혜지된 계좌일 때")
    void deleteAccount_alreadyUnregistered(){
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(ACCOUNT_ALREADY_UNREGISTERED);
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래 금액이 잔액보다 큰 경우")
    void exceedAmountUseBalance(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000000")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account)
                );

        //when
        //then
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        assertThat(accountException.getErrorCode()).isEqualTo(AMOUNT_EXCEED_BALANCE);
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void savedFailedUseTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransactions("1000000000", 200L);

        //then
        verify(transactionRepository, times(1))
                .save(transactionArgumentCaptor.capture());

        assertThat(transactionArgumentCaptor.getValue().getBalanceSnapshot()).isEqualTo(10000L);
        assertThat(transactionArgumentCaptor.getValue().getTransactionResultType()).isEqualTo(F);
        assertThat(transactionArgumentCaptor.getValue().getAmount()).isEqualTo(200L);
    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void cancelBalanceSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

        //when

        TransactionDto cancelBalanceTransactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1))
                .save(transactionArgumentCaptor.capture());


        assertThat(transactionArgumentCaptor.getValue().getBalanceSnapshot()).isEqualTo(10000L + CANCEL_AMOUNT);
        assertThat(transactionArgumentCaptor.getValue().getTransactionType()).isEqualTo(CANCEL);
        assertThat(transactionArgumentCaptor.getValue().getTransactionResultType()).isEqualTo(S);
        assertThat(transactionArgumentCaptor.getValue().getAmount()).isEqualTo(CANCEL_AMOUNT);
        assertThat(cancelBalanceTransactionDto.getAccountNumber()).isEqualTo("1000000000");
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래와 계좌의 매칭이 실패할 때")
    void cancelTransaction_AccountNotMatchedTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        Account accountNotTransaction = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000001")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotTransaction));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(TRANSACTION_ACCOUNT_UN_MATCH);
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래 금액과 취소 금액의 매칭이 실패할 때")
    void cancelTransaction_CancelMustFully(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L + CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", CANCEL_AMOUNT));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(CANCEL_MUST_FULLY);
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래 금액과 취소 금액의 매칭이 실패할 때")
    void cancelTransaction_TooOldToCancel(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", CANCEL_AMOUNT));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(TOO_OLD_TRANSACTION_TO_CANCEL);
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancelTransaction_TransactionNotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "1000000000",
                        CANCEL_AMOUNT)
        );

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(TRANSACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelTransaction_AccountNotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("잔금 사용 내역 확인 성공")
    void successQueryTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        //when
        TransactionDto transactionDto = transactionService.queryTransaction("transactionId");

        //then
        assertThat(transactionDto.getTransactionType()).isEqualTo(USE);
        assertThat(transactionDto.getTransactionResultType()).isEqualTo(S);
        assertThat(transactionDto.getTransactionId()).isEqualTo("transactionId");
        assertThat(transactionDto.getAmount()).isEqualTo(CANCEL_AMOUNT);
        assertThat(transactionDto.getAccountNumber()).isEqualTo("1000000000");
    }

    @Test
    @DisplayName("잔액 사용 내역 조회 실패 - 해당 거래 없음")
    void queryTransaction_TransactionNotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction(
                        "transactionId"
                )
        );

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(TRANSACTION_NOT_FOUND);
    }
}