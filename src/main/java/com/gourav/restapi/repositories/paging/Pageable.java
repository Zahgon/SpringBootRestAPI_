package com.gourav.restapi.repositories.paging;

/**
 * Abstract pagination information handed to a repository query.
 */
public interface Pageable {

    int getPageNumber();

    int getPageSize();

    long getOffset();

    Sort getSort();
}
