package org.chengyuan.cyproject.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagination {

    private Integer page;

    private Integer pageSize;

    private Long totalItems;

    private Integer totalPages;

    /** 根据总数自动计算总页数，避免各处重复手写 */
    public static Pagination of(int page, int pageSize, long totalItems) {
        int safePageSize = pageSize <= 0 ? 1 : pageSize;
        int totalPages = (int) Math.ceil((double) totalItems / safePageSize);
        return new Pagination(page, safePageSize, totalItems, totalPages);
    }
}
