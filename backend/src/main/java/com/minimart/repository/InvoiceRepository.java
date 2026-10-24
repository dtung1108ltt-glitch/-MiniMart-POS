package com.minimart.repository;

import com.minimart.domain.Invoice;
import com.minimart.domain.InvoiceStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate >= :from AND i.invoiceDate <= :to ORDER BY i.invoiceDate DESC")
    List<Invoice> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Invoice> findByStatusOrderByInvoiceDateDesc(InvoiceStatus status);
}
