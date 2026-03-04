package com.vfu.chatbot.ai;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PolicyDocumentLoader {

    private final VectorStore vectorStore;

    public PolicyDocumentLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadPoliciesIfNotExists() {
        try {
            // Check if policies already loaded
            List<Document> sample = vectorStore.similaritySearch(
                    SearchRequest.builder().query("Policies").topK(1).build()
            );

            if (!sample.isEmpty()) {
                log.info("✅ Policies already loaded");
                return;
            }

            log.info("📤 Loading policies (first time)");
            loadPolicies();

        } catch (Exception e) {
            log.error("Policy check failed", e);
        }
    }

    public void loadPolicies() {
        try {
            List<Document> generalPoliciesDoc = new PagePdfDocumentReader("classpath:/rag-documents/VFY General Policies.pdf",
                    PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build())
                            .withPagesPerDocument(1)
                            .build()).read();
            generalPoliciesDoc.forEach(doc -> {
                doc.getMetadata().put("doc_type", "general");
                doc.getMetadata().put("source", "VFY General Policies.pdf");
            });

            List<Document> reservationPoliciesDoc = new PagePdfDocumentReader("classpath:/rag-documents/VFY Reservation Policies.pdf",
                    PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build())
                            .withPagesPerDocument(1)
                            .build()).read();

            reservationPoliciesDoc.forEach(doc -> {
                doc.getMetadata().put("doc_type", "reservation");
                doc.getMetadata().put("source", "VFY Reservation Policies.pdf");
            });


            // Add all documents → this triggers embedding + pgVector insert
            vectorStore.add(reservationPoliciesDoc);
            vectorStore.add(generalPoliciesDoc);

            log.info("Loaded {} reservation policy pages and {} general policy pages into pgVector",
                    reservationPoliciesDoc.size(), generalPoliciesDoc.size());
        } catch (Exception e) {
            log.error("Failed to load policy PDFs into pgVector", e);
        }

    }
}

