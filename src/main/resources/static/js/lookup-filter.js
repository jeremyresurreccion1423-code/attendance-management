/**
 * Admin cascading lookups — always fetches fresh data from Supabase (PostgreSQL).
 */
(function () {
    const API = '/api/v1/admin/lookups';

    async function fetchJson(url) {
        const response = await fetch(url, { headers: { Accept: 'application/json' }, cache: 'no-store' });
        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Lookup request failed');
        }
        return response.json();
    }

    function fillSelect(select, items, placeholder, valueKey, labelKey) {
        if (!select) return;
        const current = select.value;
        select.innerHTML = '';
        const empty = document.createElement('option');
        empty.value = '';
        empty.textContent = placeholder || '-- Select --';
        select.appendChild(empty);
        items.forEach((item) => {
            const option = document.createElement('option');
            if (typeof item === 'string') {
                option.value = item;
                option.textContent = item;
            } else {
                option.value = String(item[valueKey || 'id']);
                option.textContent = item[labelKey || 'label'];
            }
            select.appendChild(option);
        });
        if (current && Array.from(select.options).some((opt) => opt.value === current)) {
            select.value = current;
        }
    }

    function getDepartmentId(form) {
        const deptSelect = form.querySelector('[data-lookup-role="department"]');
        return deptSelect && deptSelect.value ? deptSelect.value : null;
    }

    async function refreshDepartments(scope) {
        const root = scope || document;
        const selects = root.querySelectorAll('[data-lookup-role="department"]');
        if (!selects.length) return;
        const departments = await fetchJson(`${API}/departments`);
        selects.forEach((select) => {
            const placeholder = select.getAttribute('data-placeholder') || '-- Select Department --';
            fillSelect(select, departments, placeholder);
        });
    }

    async function refreshYearLevels(form) {
        const departmentId = getDepartmentId(form);
        const yearSelect = form.querySelector('[data-lookup-role="year-level"]');
        if (!yearSelect) return;
        if (!departmentId) {
            fillSelect(yearSelect, [], '-- Select department first --');
            return;
        }
        const levels = await fetchJson(`${API}/year-levels?departmentId=${departmentId}`);
        fillSelect(yearSelect, levels, '-- Select Year Level --');
    }

    async function refreshTeachers(form) {
        const departmentId = getDepartmentId(form);
        const teacherSelect = form.querySelector('[data-lookup-role="teacher"]');
        if (!teacherSelect) return;
        if (!departmentId) {
            fillSelect(teacherSelect, [], '-- Select department first --');
            return;
        }
        const teachers = await fetchJson(`${API}/teachers?departmentId=${departmentId}`);
        fillSelect(teacherSelect, teachers, '-- Select Teacher --');
    }

    async function refreshSections(form, yearLevel) {
        const departmentId = getDepartmentId(form);
        const sectionSelect = form.querySelector('[data-lookup-role="section"]');
        if (!sectionSelect) return;
        if (!departmentId) {
            fillSelect(sectionSelect, [], '-- Select department first --');
            return;
        }
        const yearSelect = form.querySelector('[data-lookup-role="year-level"]');
        const effectiveYear = yearLevel || (yearSelect ? yearSelect.value : null);
        let url = `${API}/sections?departmentId=${departmentId}`;
        if (effectiveYear) {
            url += `&yearLevel=${encodeURIComponent(effectiveYear)}`;
        }
        const sections = await fetchJson(url);
        const placeholder = effectiveYear ? '-- Select Section --' : '-- Select Section --';
        fillSelect(sectionSelect, sections, placeholder);
    }

    async function refreshFormLookups(form) {
        await refreshDepartments(form);
        const departmentId = getDepartmentId(form);
        if (!departmentId) return;
        await refreshYearLevels(form);
        await refreshTeachers(form);
        const yearSelect = form.querySelector('[data-lookup-role="year-level"]');
        await refreshSections(form, yearSelect ? yearSelect.value : null);
    }

    function bindForm(form) {
        const deptSelect = form.querySelector('[data-lookup-role="department"]');
        const yearSelect = form.querySelector('[data-lookup-role="year-level"]');

        if (deptSelect) {
            deptSelect.addEventListener('change', async () => {
                try {
                    await refreshYearLevels(form);
                    await refreshTeachers(form);
                    await refreshSections(form, yearSelect ? yearSelect.value : null);
                } catch (error) {
                    console.error(error);
                }
            });
        }

        if (yearSelect) {
            yearSelect.addEventListener('change', async () => {
                try {
                    await refreshSections(form, yearSelect.value);
                } catch (error) {
                    console.error(error);
                }
            });
        }

        refreshFormLookups(form).catch(console.error);
    }

    function init() {
        document.querySelectorAll('form[data-lookup-form]').forEach(bindForm);
        document.querySelectorAll('[data-lookup-role="department"]:not(form [data-lookup-role="department"])').forEach((select) => {
            refreshDepartments(select.closest('form') || document).catch(console.error);
        });

        document.querySelectorAll('.create-tab').forEach((tab) => {
            tab.addEventListener('click', () => {
                refreshDepartments(document).catch(console.error);
            });
        });

        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                refreshDepartments(document).catch(console.error);
            }
        });
    }

    window.adminLookups = {
        refreshDepartments,
        refreshFormLookups,
        bindForm,
    };

    document.addEventListener('DOMContentLoaded', init);
})();
