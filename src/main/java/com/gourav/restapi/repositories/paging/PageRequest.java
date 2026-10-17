package com.gourav.restapi.repositories.paging;

/**
 * Immutable {@link Pageable} implementation.
 *
 * <p>The page index must be zero or greater and the page size must be one or greater; anything
 * else is rejected with an {@link IllegalArgumentException}.</p>
 */
public final class PageRequest implements Pageable {

    private final int page;
    private final int size;
    private final Sort sort;

    private PageRequest(int page, int size, Sort sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }
        return new PageRequest(page, size, sort);
    }

    @Override
    public int getPageNumber() {
        return page;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return (long) page * (long) size;
    }

    @Override
    public Sort getSort() {
        return sort;
    }
}
