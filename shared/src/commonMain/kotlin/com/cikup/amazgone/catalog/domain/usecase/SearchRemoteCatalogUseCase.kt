package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.repository.CatalogRepository

/** When online, asks upstream APIs for more matches; results land in Room and flow into search. */
class SearchRemoteCatalogUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(query: String) {
        val trimmed = query.trim()
        if (trimmed.length >= MIN_QUERY_LENGTH) repository.searchRemote(trimmed)
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }
}
