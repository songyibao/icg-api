package com.yb.icgapi.common;

import lombok.Data;

@Data
public class PageRequest {
        private int currentPage = 1;
        private int pageSize = 10;
        private String sortField;
        private String sortOrder = "desc";
}
