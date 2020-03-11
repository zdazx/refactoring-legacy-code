package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    public static final int MAX_PAY_TIME = 20 * 24 * 3600 * 1000;
    public static final String PREFIX_OF_TRANS_ID = "t_";
    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private Long createdTimestamp;
    private Double amount;
    private STATUS status;
    private String walletTransactionId;
    private RedisDistributedLock distributedLock;
    private WalletService walletService;

    public void setDistributedLock(RedisDistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    public void setWalletService(WalletService walletService) {
        this.walletService = walletService;
    }

    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Long productId, String orderId, Double amount) {
        setId(preAssignedId);
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.orderId = orderId;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
        this.amount = amount;
    }

    private void setId(String preAssignedId) {
        if (preAssignedId != null && !preAssignedId.isEmpty()) {
            this.id = preAssignedId;
        } else {
            this.id = IdGenerator.generateTransactionId();
        }
        if (!this.id.startsWith(PREFIX_OF_TRANS_ID)) {
            this.id = PREFIX_OF_TRANS_ID + preAssignedId;
        }
    }

    public boolean execute() throws InvalidTransactionException {
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (isPayed()) { return true; }
        boolean isLocked = false;
        try {
            isLocked = distributedLock.lock(id);

            if (!isLocked) { return false; }
            if (isPayed()) { return true; }
            if (isExpired()) {
                this.status = STATUS.EXPIRED;
                return false;
            }
            return pay();
        } finally {
            if (isLocked) {
                distributedLock.unlock(id);
            }
        }
    }

    private boolean isPayed() {
        return status == STATUS.EXECUTED;
    }

    private boolean pay() {
        String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
        if (walletTransactionId != null) {
            this.walletTransactionId = walletTransactionId;
            this.status = STATUS.EXECUTED;
            return true;
        } else {
            this.status = STATUS.FAILED;
            return false;
        }
    }

    private boolean isExpired() {
        return System.currentTimeMillis() - createdTimestamp > MAX_PAY_TIME;
    }

}