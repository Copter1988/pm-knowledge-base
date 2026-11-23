// 全局交互增强（不涉及业务功能逻辑）

// 防止表单重复提交：提交瞬间禁用提交按钮
document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form || form.tagName !== 'FORM') {
        return;
    }
    var btn = form.querySelector('button[type="submit"], input[type="submit"]');
    if (btn) {
        btn.disabled = true;
    }
}, true);
