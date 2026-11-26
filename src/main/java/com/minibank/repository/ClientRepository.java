package com.minibank.repository;

import com.minibank.db.JpaUtil;
import com.minibank.entity.Client;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.sql.SQLException;
import java.util.Optional;

public class ClientRepository {

    public Client save(Client client) {
        EntityManager entityManager = JpaUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(client);
            transaction.commit();
            return client;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    public Optional<Client> findByClientId(Long clientId) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return Optional.ofNullable(em.find(Client.class, clientId));
        }
    }

    public boolean clientIsExist(long clientId) throws SQLException {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return em.find(Client.class, clientId) != null;
        }
    }
}
