package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.AccountRepository;
import com.example.account.type.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.account.type.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                                .accountUser(user)
                                .accountNumber("1000000012")
                        .build()));

        // 어떤 계좌번호를 넣어도 통과된다 이를 검증할 수 있는 방법을 고려
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013")
                        .build());

        // ArgumentCaptor 로 검증
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertThat(accountDto.getUserId()).isEqualTo(12L);

        // 반드시 가장 최근에 만든 계좌번호의 다음 번호가 되어야 한다.
        assertThat(accountArgumentCaptor.getValue().getAccountNumber()).isEqualTo("1000000013");
    }

    @Test
    void createFirstAccount(){
        //given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        // 어떤 계좌번호를 넣어도 통과된다 이를 검증할 수 있는 방법을 고려
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013")
                        .build());

        // ArgumentCaptor 로 검증
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertThat(accountDto.getUserId()).isEqualTo(15L);

        // 반드시 가장 최근에 만든 계좌번호의 다음 번호가 되어야 한다.
        assertThat(accountArgumentCaptor.getValue().getAccountNumber()).isEqualTo("1000000000");
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    @DisplayName("계좌 혜지 성공")
    void deleteAccountSuccess(){
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertThat(accountDto.getUserId()).isEqualTo(12L);
        assertThat(accountArgumentCaptor.getValue().getAccountNumber()).isEqualTo("1000000012");
        assertThat(accountArgumentCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.UNREGISTERED);
    }

    @Test
    @DisplayName("계좌 혜지 실패 - 해당 유저 없음")
    void deleteAccount_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    @DisplayName("계좌 혜지 실패 - 해당 계좌 없음")
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
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("계좌 혜지 실패 - 계좌 소유주 다름")
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
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_ACCOUNT_UN_MATCH);
    }

    @Test
    @DisplayName("계좌 혜지 실패 - 잔금이 남아있을 때")
    void deleteAccount_balanceNotEmpty(){
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
                        .balance(100L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(BALANCE_NOT_EMPTY);
    }

    @Test
    @DisplayName("계좌 혜지 실패 - 이미 혜지된 계좌일 때")
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
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(ACCOUNT_ALREADY_UNREGISTERED);
    }

    @Test
    @DisplayName("유저 아이디로 계좌 정보 불러오기 성공")
    void successGetAccountsByUserId(){
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1111111111")
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("2222222222")
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("3333333333")
                        .balance(3000L)
                        .build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when
        List<AccountDto> accountsDtos = accountService.getAccountsByUserId(1L);

        //then
        assertThat(accountsDtos.size()).isEqualTo(3);

        assertThat(accountsDtos.get(0).getAccountNumber()).isEqualTo("1111111111");
        assertThat(accountsDtos.get(0).getBalance()).isEqualTo(1000);

        assertThat(accountsDtos.get(1).getAccountNumber()).isEqualTo("2222222222");
        assertThat(accountsDtos.get(1).getBalance()).isEqualTo(2000);

        assertThat(accountsDtos.get(2).getAccountNumber()).isEqualTo("3333333333");
        assertThat(accountsDtos.get(2).getBalance()).isEqualTo(3000);
    }

    @Test
    void getAccountsByUserId_NoUser(){
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개까지")
    void createAccount_maxAccountIs10(){
        //given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertThat(accountException.getErrorCode()).isEqualTo(MAX_ACCOUNT_PER_USER_10);
    }
}