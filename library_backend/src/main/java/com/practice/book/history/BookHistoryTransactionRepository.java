package com.practice.book.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookHistoryTransactionRepository extends
        JpaRepository<BookHistoryTransaction,Integer>,
        JpaSpecificationExecutor<BookHistoryTransaction> {

    @Query("""
        SELECT history FROM BookHistoryTransaction history
            WHERE history.user.id = :userId
    """)
    Page<BookHistoryTransaction> findAllBorrowedBooks(Pageable pageable, Integer userId);

    @Query("""
        SELECT history FROM BookHistoryTransaction history
            WHERE history.book.createdBy = :userId
    """)
    Page<BookHistoryTransaction> findAllReturnedBooks(Pageable pageable, Integer userId);

    @Query("""
            SELECT
            (COUNT (*) > 0) AS isBorrowed
            FROM BookHistoryTransaction bookTransactionHistory
            WHERE bookTransactionHistory.user.id = :userId
            AND bookTransactionHistory.book.id = :bookId
            AND bookTransactionHistory.returnApproved = false
            """)
    boolean isAlreadyBorrowedByUser(Integer bookId, Integer userId);

    @Query("""
            SELECT transaction
            FROM BookHistoryTransaction  transaction
            WHERE transaction.user.id = :userId
            AND transaction.book.id = :bookId
            AND transaction.returned = false
            AND transaction.returnApproved = false
            """)
    Optional<BookHistoryTransaction> findByBookIdAndUserId(Integer bookId, Integer userId);


    @Query("""
            SELECT transaction
            FROM BookHistoryTransaction  transaction
            WHERE transaction.book.owner.id = :userId
            AND transaction.book.id = :bookId
            AND transaction.returned = true 
            AND transaction.returnApproved = false
            """)
    Optional<BookHistoryTransaction> findByBookIdAndOwnerId(Integer bookId, Integer userId);
}
