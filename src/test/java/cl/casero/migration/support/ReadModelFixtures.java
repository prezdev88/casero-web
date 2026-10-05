package cl.casero.migration.support;

import java.util.List;

import cl.casero.migration.domain.AppUser;
import cl.casero.migration.domain.Customer;
import cl.casero.migration.domain.Transaction;
import cl.casero.migration.domain.enums.UserRole;
import cl.casero.migration.service.dto.CustomerDetails;
import cl.casero.migration.service.dto.CustomerScoreInput;
import cl.casero.migration.service.dto.TransactionDetails;
import cl.casero.migration.service.dto.UserIdentity;
import cl.casero.migration.service.mapping.ReadModelMapper;
import cl.casero.migration.web.security.CaseroUserDetails;

public final class ReadModelFixtures {

    private ReadModelFixtures() {}

    public static UserIdentity identity(AppUser user) {
        if (user == null) {
            return null;
        }

        Long id = user.getId();
        String name = user.getName();
        UserRole role = user.getRole();
        boolean enabled = user.isEnabled();
        return new UserIdentity(id, name, role, enabled);
    }

    public static CaseroUserDetails principal(AppUser user) {
        UserIdentity identity = identity(user);
        String username = user.getPinFingerprint();
        String password = user.getPinHash();
        return new CaseroUserDetails(identity, username, password);
    }

    public static CustomerDetails customer(Customer entity) {
        return ReadModelMapper.customer(entity);
    }

    public static List<CustomerDetails> customers(List<Customer> entities) {
        return entities.stream().map(ReadModelMapper::customer).toList();
    }

    public static List<TransactionDetails> transactions(List<Transaction> entities) {
        return entities.stream().map(ReadModelMapper::transaction).toList();
    }

    public static List<CustomerScoreInput> scores(List<Customer> entities) {
        return entities.stream().map(entity -> {
            Long id = entity.getId();
            Integer debt = entity.getDebt();
            return new CustomerScoreInput(id, debt);
        }).toList();
    }
}
