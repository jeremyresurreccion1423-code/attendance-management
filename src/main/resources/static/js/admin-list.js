/**
 * Client-side search, filter, sort, and pagination for admin tables.
 */
(function () {
    function parseStatus(value) {
        return value ? value.trim().toUpperCase() : '';
    }

    function initAdminTable(table, options) {
        if (!table) return;

        const tbody = table.tBodies[0];
        if (!tbody) return;

        const allRows = Array.from(tbody.querySelectorAll('tr[data-admin-row]'));
        const searchInput = options.searchInput;
        const statusFilter = options.statusFilter;
        const pageSizeSelect = options.pageSizeSelect;
        const paginationEl = options.paginationEl;
        const countEl = options.countEl;
        let sortKey = null;
        let sortAsc = true;
        let page = 1;
        let pageSize = Number(pageSizeSelect?.value || options.defaultPageSize || 10);

        function rowStatus(row) {
            return parseStatus(row.getAttribute('data-status') || '');
        }

        function rowSearchText(row) {
            return (row.getAttribute('data-search') || row.textContent || '').toLowerCase();
        }

        function getFilteredRows() {
            const query = (searchInput?.value || '').trim().toLowerCase();
            const status = parseStatus(statusFilter?.value || '');
            let rows = allRows.filter((row) => {
                const matchesSearch = !query || rowSearchText(row).includes(query);
                const matchesStatus = !status || rowStatus(row) === status;
                return matchesSearch && matchesStatus;
            });

            if (sortKey) {
                rows = rows.slice().sort((a, b) => {
                    const av = (a.getAttribute(`data-${sortKey}`) || '').toLowerCase();
                    const bv = (b.getAttribute(`data-${sortKey}`) || '').toLowerCase();
                    if (av < bv) return sortAsc ? -1 : 1;
                    if (av > bv) return sortAsc ? 1 : -1;
                    return 0;
                });
            }
            return rows;
        }

        function renderPagination(totalPages) {
            if (!paginationEl) return;
            paginationEl.innerHTML = '';
            if (totalPages <= 1) return;

            const prev = document.createElement('button');
            prev.type = 'button';
            prev.className = 'btn btn-secondary btn-sm';
            prev.textContent = 'Prev';
            prev.disabled = page <= 1;
            prev.addEventListener('click', () => {
                page = Math.max(1, page - 1);
                render();
            });

            const label = document.createElement('span');
            label.className = 'admin-page-label';
            label.textContent = `Page ${page} of ${totalPages}`;

            const next = document.createElement('button');
            next.type = 'button';
            next.className = 'btn btn-secondary btn-sm';
            next.textContent = 'Next';
            next.disabled = page >= totalPages;
            next.addEventListener('click', () => {
                page = Math.min(totalPages, page + 1);
                render();
            });

            paginationEl.appendChild(prev);
            paginationEl.appendChild(label);
            paginationEl.appendChild(next);
        }

        function pairedEditRow(row) {
            const next = row.nextElementSibling;
            if (next && next.id && next.id.startsWith('edit-row-')) {
                return next;
            }
            return null;
        }

        function render() {
            const filtered = getFilteredRows();
            const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
            if (page > totalPages) page = totalPages;

            allRows.forEach((row) => {
                row.style.display = 'none';
                const editRow = pairedEditRow(row);
                if (editRow && editRow.dataset.editOpen !== '1') {
                    editRow.style.display = 'none';
                }
            });

            const start = (page - 1) * pageSize;
            const visible = filtered.slice(start, start + pageSize);
            visible.forEach((row) => {
                row.style.display = '';
                const editRow = pairedEditRow(row);
                if (editRow && editRow.dataset.editOpen === '1') {
                    editRow.style.display = 'table-row';
                }
            });

            if (countEl) {
                countEl.textContent = `${filtered.length} record(s)`;
            }
            renderPagination(totalPages);
        }

        if (searchInput) {
            searchInput.addEventListener('input', () => {
                page = 1;
                render();
            });
        }
        if (statusFilter) {
            statusFilter.addEventListener('change', () => {
                page = 1;
                render();
            });
        }
        if (pageSizeSelect) {
            pageSizeSelect.addEventListener('change', () => {
                pageSize = Number(pageSizeSelect.value || 10);
                page = 1;
                render();
            });
        }

        table.querySelectorAll('th[data-sort]').forEach((header) => {
            header.style.cursor = 'pointer';
            header.addEventListener('click', () => {
                const key = header.getAttribute('data-sort');
                if (sortKey === key) {
                    sortAsc = !sortAsc;
                } else {
                    sortKey = key;
                    sortAsc = true;
                }
                render();
            });
        });

        render();
    }

    window.initAdminTable = initAdminTable;
})();
