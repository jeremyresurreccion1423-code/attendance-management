/**
 * Cascading department lookups for create forms.
 * Requires department select with data-lookup-role="department".
 */
(function () {
    const API = '/api/v1/admin/lookups';

    async function fetchJson(url) {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Lookup request failed');
        }
        return response.json();
    }

    function fillSelect(select, items, placeholder) {
        if (!select) return;
        const current = select.value;
        select.innerHTML = '';
        const empty = document.createElement('option');
        empty.value = '';
        empty.textContent = placeholder || '-- Select --';
        select.appendChild(empty);
        items.forEach((item) => {
            const option = document.createElement('option');
            option.value = item.id;
            option.textContent = item.label;
            select.appendChild(option);
        });
        if (current) {
            select.value = current;
        }
    }

    function getDepartmentId(form) {
        const deptSelect = form.querySelector('[data-lookup-role="department"]');
        return deptSelect && deptSelect.value ? deptSelect.value : null;
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
        fillSelect(teacherSelect, teachers, '-- Select --');
    }

    async function refreshSections(form, yearLevel) {
        const departmentId = getDepartmentId(form);
        const sectionSelect = form.querySelector('[data-lookup-role="section"]');
        if (!sectionSelect) return;
        if (!departmentId) {
            fillSelect(sectionSelect, [], '-- Select department first --');
            return;
        }
        let url = `${API}/sections?departmentId=${departmentId}`;
        if (yearLevel) {
            url += `&yearLevel=${encodeURIComponent(yearLevel)}`;
        }
        const sections = await fetchJson(url);
        fillSelect(sectionSelect, sections, '-- Select --');
    }

    function bindForm(form) {
        const deptSelect = form.querySelector('[data-lookup-role="department"]');
        const yearSelect = form.querySelector('[data-lookup-role="year-level"]');
        if (deptSelect) {
            deptSelect.addEventListener('change', async () => {
                try {
                    await refreshTeachers(form);
                    const year = yearSelect ? yearSelect.value : null;
                    await refreshSections(form, year);
                } catch (e) {
                    console.error(e);
                }
            });
        }
        if (yearSelect) {
            yearSelect.addEventListener('change', async () => {
                try {
                    await refreshSections(form, yearSelect.value);
                } catch (e) {
                    console.error(e);
                }
            });
        }
        if (deptSelect && deptSelect.value) {
            refreshTeachers(form).catch(console.error);
            const year = yearSelect ? yearSelect.value : null;
            refreshSections(form, year).catch(console.error);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('form[data-lookup-form]').forEach(bindForm);
    });
})();
