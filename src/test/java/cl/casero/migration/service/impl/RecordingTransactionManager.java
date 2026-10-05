package cl.casero.migration.service.impl;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    private int beginCount;
    private int commitCount;
    private int rollbackCount;
    private boolean readOnly;
    private boolean active;

    int getBeginCount() {
        return beginCount;
    }

    int getCommitCount() {
        return commitCount;
    }

    int getRollbackCount() {
        return rollbackCount;
    }

    boolean isReadOnly() {
        return readOnly;
    }

    @Override
    protected Object doGetTransaction() {
        return new Object();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return active;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        beginCount++;
        readOnly = definition.isReadOnly();
        active = true;
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        commitCount++;
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        rollbackCount++;
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        active = false;
    }
}
