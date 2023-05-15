package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.repository.AccountRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("get account success")
    void testXXX(){
        //given
        given(accountRepository.findById(anyLong()))
                .willReturn(Optional.of(Account.builder()
                                .accountStatus(AccountStatus.UNREGISTERED)
                                .accountNumber("65789").build()));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);


        //when
        Account account = accountService.getAccount(4555L);

        //then
        verify(accountRepository, times(1)).findById(captor.capture());
        verify(accountRepository, times(0)).save(any());

        assertThat(4555L).isEqualTo(captor.getValue());
        assertThat(45515L).isNotEqualTo(captor.getValue());
        assertTrue(4555L == captor.getValue());

        assertThat("65789").isEqualTo(account.getAccountNumber());
        assertThat(AccountStatus.UNREGISTERED).isEqualTo(account.getAccountStatus());
    }

    @Test
    @DisplayName("get account failed")
    void testFailedToSearchAccount(){
        //given

        //when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.getAccount(-10L));

        //then

        assertThat("Minus").isEqualTo(exception.getMessage());
    }

    @Test
    void createAccount() {
    }

    @Test
    @DisplayName("get account")
    void getAccount() {
        //given
        given(accountRepository.findById(anyLong()))
                .willReturn(Optional.of(Account.builder()
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("65789").build()));


        //when
        Account account = accountService.getAccount(4555L);

        //then
        assertThat("65789").isEqualTo(account.getAccountNumber());
        assertThat(AccountStatus.UNREGISTERED).isEqualTo(account.getAccountStatus());
    }
}