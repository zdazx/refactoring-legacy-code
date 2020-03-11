package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepository;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.transaction.InvalidTransactionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class WalletTransactionTest {

    @Mock
    private RedisDistributedLock redisDistributedLock = RedisDistributedLock.getSingletonInstance();

    @Mock
    private UserRepository userRepo;

    @Mock
    User buyer = new User();

    @Mock
    User seller = new User();

    WalletServiceImpl walletService = new WalletServiceImpl();

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(buyer.getBalance()).thenReturn(10D);
        when(seller.getBalance()).thenReturn(10D);
        when(userRepo.find(1L)).thenReturn(buyer);
        when(userRepo.find(2L)).thenReturn(seller);
        walletService.setUserRepository(userRepo);
    }

    @Test
    public void should_return_true_when_all_param_is_valid() throws InvalidTransactionException {
        when(redisDistributedLock.lock(any(String.class))).thenReturn(true);
        doNothing().when(redisDistributedLock).unlock(any(String.class));

        WalletTransaction transaction = new WalletTransaction(null, 1L, 2L, 999L, "123456", 1D);
        transaction.setDistributedLock(redisDistributedLock);
        transaction.setWalletService(walletService);

        assertTrue(transaction.execute());
    }

    @Test
    public void should_return_false_when_amount_is_less_than_balance() throws InvalidTransactionException {
        when(redisDistributedLock.lock(any(String.class))).thenReturn(true);
        doNothing().when(redisDistributedLock).unlock(any(String.class));

        WalletTransaction transaction = new WalletTransaction(null, 1L, 2L, 999L, "123456", 20D);
        transaction.setDistributedLock(redisDistributedLock);
        transaction.setWalletService(walletService);

        assertFalse(transaction.execute());
    }

    @Test
    public void should_return_false_when_user_is_locked() throws InvalidTransactionException {
        when(redisDistributedLock.lock(any(String.class))).thenReturn(false);
        doNothing().when(redisDistributedLock).unlock(any(String.class));

        WalletTransaction transaction = new WalletTransaction(null, 1L, 2L, 999L, "123456", 20D);
        transaction.setDistributedLock(redisDistributedLock);
        transaction.setWalletService(walletService);

        assertFalse(transaction.execute());

    }
}