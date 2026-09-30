// SwiftLab Invoicing UI Scripts
(function () {
    // -------------------------------------------------------------
    // Theme Management (Light / Dark)
    // -------------------------------------------------------------
    const THEME_KEY = 'swiftlab_invoice_theme';

    function initTheme() {
        const savedTheme = localStorage.getItem(THEME_KEY) || 'light';
        applyTheme(savedTheme);
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(THEME_KEY, theme);

        const iconContainer = document.getElementById('theme-icon');
        const themeText = document.getElementById('theme-text');

        if (iconContainer) {
            if (theme === 'dark') {
                iconContainer.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`;
                if (themeText) themeText.textContent = 'Light';
            } else {
                iconContainer.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
                if (themeText) themeText.textContent = 'Dark';
            }
        }
    }

    window.toggleTheme = function () {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
    };

    initTheme();

    document.addEventListener('DOMContentLoaded', () => {
        initTheme();

        const themeBtn = document.getElementById('theme-toggle-btn');
        if (themeBtn) {
            themeBtn.addEventListener('click', window.toggleTheme);
        }

        initInvoiceFormCalculations();
    });

    // -------------------------------------------------------------
    // Dynamic Invoice Item Calculations with Configured Currency
    // -------------------------------------------------------------
    function initInvoiceFormCalculations() {
        const itemsContainer = document.getElementById('invoice-items-body');
        if (!itemsContainer) return;

        function getCurrencySymbol() {
            return document.body.getAttribute('data-currency') || '₹';
        }

        function recalculate() {
            const curr = getCurrencySymbol();
            let subtotal = 0;
            const rows = itemsContainer.querySelectorAll('tr.item-row');

            rows.forEach((row, index) => {
                const qtyInput = row.querySelector('.item-qty');
                const priceInput = row.querySelector('.item-price');
                const totalDisplay = row.querySelector('.item-line-total');

                const qty = parseFloat(qtyInput ? qtyInput.value : 0) || 0;
                const price = parseFloat(priceInput ? priceInput.value : 0) || 0;
                const lineTotal = qty * price;

                if (totalDisplay) {
                    totalDisplay.textContent = curr + lineTotal.toFixed(2);
                }
                subtotal += lineTotal;

                const descInput = row.querySelector('.item-desc');
                if (descInput) descInput.name = `items[${index}].description`;
                if (qtyInput) qtyInput.name = `items[${index}].quantity`;
                if (priceInput) priceInput.name = `items[${index}].unitPrice`;
            });

            const taxRateInput = document.getElementById('tax-rate');
            const discountInput = document.getElementById('discount');

            const taxRate = parseFloat(taxRateInput ? taxRateInput.value : 0) || 0;
            const discount = parseFloat(discountInput ? discountInput.value : 0) || 0;

            const base = Math.max(0, subtotal - discount);
            const taxAmount = base * (taxRate / 100.0);
            const grandTotal = base + taxAmount;

            const dispSubtotal = document.getElementById('display-subtotal');
            const dispTax = document.getElementById('display-tax');
            const dispGrand = document.getElementById('display-grand-total');

            if (dispSubtotal) dispSubtotal.textContent = curr + subtotal.toFixed(2);
            if (dispTax) dispTax.textContent = curr + taxAmount.toFixed(2);
            if (dispGrand) dispGrand.textContent = curr + grandTotal.toFixed(2);
        }

        window.recalculateInvoice = recalculate;

        itemsContainer.addEventListener('input', recalculate);
        const taxRateInput = document.getElementById('tax-rate');
        const discountInput = document.getElementById('discount');
        if (taxRateInput) taxRateInput.addEventListener('input', recalculate);
        if (discountInput) discountInput.addEventListener('input', recalculate);

        window.addItemRow = function () {
            const curr = getCurrencySymbol();
            const rowCount = itemsContainer.querySelectorAll('tr.item-row').length;
            const tr = document.createElement('tr');
            tr.className = 'item-row';
            tr.innerHTML = `
                <td>
                    <input type="text" name="items[${rowCount}].description" class="form-control item-desc" placeholder="Item / Service description" required />
                </td>
                <td style="width: 110px;">
                    <input type="number" name="items[${rowCount}].quantity" class="form-control item-qty" value="1" min="1" step="1" required />
                </td>
                <td style="width: 170px;">
                    <input type="number" name="items[${rowCount}].unitPrice" class="form-control item-price" value="0.00" min="0" step="0.01" required />
                </td>
                <td style="width: 140px; text-align: right; font-weight: 600;" class="item-line-total">${curr}0.00</td>
                <td style="width: 50px; text-align: center;">
                    <button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)" title="Remove item">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    </button>
                </td>
            `;
            itemsContainer.appendChild(tr);
            recalculate();
        };

        window.removeItemRow = function (btn) {
            const rows = itemsContainer.querySelectorAll('tr.item-row');
            if (rows.length <= 1) {
                alert('An invoice must have at least one item.');
                return;
            }
            btn.closest('tr').remove();
            recalculate();
        };

        recalculate();
    }
})();
