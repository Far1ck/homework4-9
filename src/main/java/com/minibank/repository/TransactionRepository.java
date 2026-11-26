package com.minibank.repository;

import com.minibank.db.JpaUtil;
import com.minibank.entity.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class TransactionRepository {

    public Transaction save(Transaction transaction) {
        EntityManager entityManager = JpaUtil.getEntityManager();
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.persist(transaction);
            tx.commit();
            return transaction;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    public void save(EntityManager em, Transaction transaction) {
        em.persist(transaction);
    }
}
