package com.gourav.restapi.repositories.paging;

import java.util.List;

/**
 * A single slice of a larger result set together with the metadata needed to describe it.
 */
public interface Page<T> {

    List<T> getContent();

    int getNumber();

    int getSize();

    long getTotalElements();

    int getTotalPages();

    boolean isLast();
}
