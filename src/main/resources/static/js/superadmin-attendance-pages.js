/**
 * Super Admin Attendance pages — shared UI behavior ported from Admin templates.
 * Depends on: forms.js, lookup-filter.js (adminLookups), admin-list.js (initAdminTable)
 */
(function () {
  'use strict';

  window.toggleEdit = function (id) {
    var studentForm = document.getElementById('edit-student-form-' + id);
    if (studentForm) {
      saToggleEditStudent(id);
      return;
    }
    saToggleEditRow(id);
  };

  window.saToggleEditRow = function (id) {
    var row = document.getElementById('edit-row-' + id);
    if (!row) return;
    var opening = row.style.display === 'none' || row.style.display === '';
    row.style.display = opening ? 'table-row' : 'none';
    if (opening && window.adminLookups) {
      var form = row.querySelector('form[data-lookup-form]');
      if (form) {
        window.adminLookups.bindForm(form);
        var teacherSelect = form.querySelector('[data-lookup-role="teacher"]');
        var sectionSelect = form.querySelector('[data-lookup-role="section"]');
        var teacherId = teacherSelect ? teacherSelect.getAttribute('data-selected') : null;
        var sectionId = sectionSelect ? sectionSelect.getAttribute('data-selected') : null;
        if (teacherSelect || sectionSelect) {
          window.adminLookups.refreshFormLookups(form).then(function () {
            if (teacherId && teacherSelect) teacherSelect.value = teacherId;
            if (sectionId && sectionSelect) sectionSelect.value = sectionId;
          }).catch(console.error);
        }
      }
    }
  };

  window.saToggleEditStudent = function (id) {
    var row = document.getElementById('edit-row-' + id);
    if (!row) return;
    var opening = row.style.display === 'none' || row.style.display === '';
    row.style.display = opening ? 'table-row' : 'none';
    if (opening && window.adminLookups) {
      var form = document.getElementById('edit-student-form-' + id);
      if (!form) return;
      window.adminLookups.bindForm(form);
      var deptSelect = form.querySelector('[data-lookup-role="department"]');
      var yearSelect = form.querySelector('[data-lookup-role="year-level"]');
      var sectionSelect = form.querySelector('[data-lookup-role="section"]');
      var deptId = deptSelect ? deptSelect.getAttribute('data-selected') : null;
      var year = yearSelect ? yearSelect.getAttribute('data-selected') : null;
      var sectionId = sectionSelect ? sectionSelect.getAttribute('data-selected') : null;
      window.adminLookups.refreshFormLookups(form).then(function () {
        if (deptId && deptSelect) deptSelect.value = deptId;
        if (year && yearSelect) yearSelect.value = year;
        return window.adminLookups.refreshFormLookups(form);
      }).then(function () {
        if (sectionId && sectionSelect) sectionSelect.value = sectionId;
      }).catch(console.error);
    }
  };

  window.saSwitchCreateTab = function (tabId) {
    if (!tabId) return;
    document.querySelectorAll('.create-tab').forEach(function (btn) {
      var active = btn.getAttribute('data-tab') === tabId;
      btn.classList.toggle('active', active);
      btn.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    document.querySelectorAll('.create-panel').forEach(function (panel) {
      panel.classList.toggle('active', panel.getAttribute('data-panel') === tabId);
    });
    if (window.history && window.history.replaceState) {
      history.replaceState(null, '', '#' + tabId);
    }
  };

  window.saRefreshDepartmentTags = async function () {
    var container = document.getElementById('departmentTags');
    if (!container) return;
    try {
      var response = await fetch('/api/v1/admin/lookups/departments', { cache: 'no-store' });
      if (!response.ok) return;
      var departments = await response.json();
      container.innerHTML = departments.map(function (d) {
        return '<span class="create-tag">' + d.label + '</span>';
      }).join('');
      var list = document.getElementById('departmentTagList');
      if (list) list.style.display = departments.length ? '' : 'none';
    } catch (e) {
      console.error(e);
    }
  };

  window.saBindAssignWidget = function (widget) {
    var searchInput = widget.querySelector('[data-assign-search]');
    var items = Array.from(widget.querySelectorAll('.assign-student-item'));
    var checkboxes = Array.from(widget.querySelectorAll('[data-assign-checkbox]'));
    var countEl = widget.querySelector('[data-assign-count]');
    var emptyEl = widget.querySelector('[data-assign-empty]');
    var selectAllBtn = widget.querySelector('[data-assign-select-all]');
    var clearBtn = widget.querySelector('[data-assign-clear]');

    function updateCount() {
      var selected = checkboxes.filter(function (cb) { return cb.checked; }).length;
      if (countEl) countEl.textContent = selected + ' selected';
    }

    function applySearch() {
      var query = (searchInput && searchInput.value ? searchInput.value : '').trim().toLowerCase();
      var visibleCount = 0;
      items.forEach(function (item) {
        var text = item.getAttribute('data-search-text') || '';
        var visible = !query || text.indexOf(query) !== -1;
        item.hidden = !visible;
        if (visible) visibleCount += 1;
      });
      if (emptyEl) {
        emptyEl.hidden = visibleCount > 0;
        emptyEl.textContent = items.length === 0
          ? 'No students in this section.'
          : 'No students match your search.';
      }
    }

    if (searchInput) searchInput.addEventListener('input', applySearch);
    checkboxes.forEach(function (cb) { cb.addEventListener('change', updateCount); });
    if (selectAllBtn) {
      selectAllBtn.addEventListener('click', function () {
        items.forEach(function (item) {
          if (item.hidden) return;
          var checkbox = item.querySelector('[data-assign-checkbox]');
          if (checkbox) checkbox.checked = true;
        });
        updateCount();
      });
    }
    if (clearBtn) {
      clearBtn.addEventListener('click', function () {
        checkboxes.forEach(function (cb) { cb.checked = false; });
        updateCount();
      });
    }
    widget.addEventListener('submit', function (event) {
      var selected = checkboxes.filter(function (cb) { return cb.checked; }).length;
      if (selected === 0) {
        event.preventDefault();
        if (window.amsConfirm) {
          window.amsConfirm('Pumili ng kahit isang student bago mag-assign.', {
            title: 'No Student Selected',
            okText: 'OK',
            cancelText: 'Close'
          });
        }
      }
    });
    applySearch();
    updateCount();
  };

  function initCreateDashboard() {
    if (!document.querySelector('.create-dash')) return;
    saRefreshDepartmentTags();
    document.querySelectorAll('.create-tab').forEach(function (btn) {
      btn.addEventListener('click', function () {
        saSwitchCreateTab(btn.getAttribute('data-tab'));
      });
    });
    document.querySelectorAll('[data-goto]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        saSwitchCreateTab(btn.getAttribute('data-goto'));
      });
    });
    var hash = (window.location.hash || '#department').replace('#', '');
    var valid = ['department', 'student', 'teacher', 'subject', 'section'];
    saSwitchCreateTab(valid.indexOf(hash) >= 0 ? hash : 'department');
  }

  document.addEventListener('DOMContentLoaded', function () {
    initCreateDashboard();
    document.querySelectorAll('[data-assign-widget]').forEach(saBindAssignWidget);
  });
})();
