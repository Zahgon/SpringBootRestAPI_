package com.gourav.restapi.repositories.paging;

import java.util.Collections;
import java.util.List;

/**
 * Default {@link Page} implementation backed by an already-materialised content list.
 */
public class PageImpl<T> implements Page<T> {

    private final List<T> content;
    private final Pageable pageable;
    private final long total;

    public PageImpl(List<T> content, Pageable pageable, long total) {
        this.content = content == null ? Collections.emptyList() : List.copyOf(content);
        this.pageable = pageable;
        this.total = total;
    }

    @Override
    public List<T> getContent() {
        return content;
    }

    @Override
    public int getNumber() {
        return pageable.getPageNumber();
    }

    @Override
    public int getSize() {
        return pageable.getPageSize();
    }

    @Override
    public long getTotalElements() {
        return total;
    }

    @Override
    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getSize());
    }

    @Override
    public boolean isLast() {
        return !(getNumber() + 1 < getTotalPages());
    }
}
