package com.minibank.repository;

import com.minibank.db.JpaUtil;
import com.minibank.entity.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public class AccountRepository {

    public Account save(Account account) {
        EntityManager entityManager = JpaUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(account);
            transaction.commit();
            return account;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    public Optional<Account> findByIdForUpdate(EntityManager em, Long id) {
        return Optional.ofNullable(em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public Optional<Account> findById(Long accountId) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            Account account = em.createQuery("SELECT a FROM Account a JOIN FETCH a.client WHERE a.id = :id", Account.class)
                    .setParameter("id", accountId)
                    .getSingleResult();
            return Optional.of(account);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
