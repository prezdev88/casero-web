# Duplicated Code Analysis

This document identifies areas of duplicated code in the casero-web project and provides recommendations for refactoring.

## Summary

| Priority | Duplication Area | Location | Impact |
|----------|-----------------|----------|--------|
| High | Pagination Sanitization | Multiple Controllers | Code maintenance, consistency |
| Medium | Form Action Methods | CustomerController | Code maintenance |
| High | Clamp Helper Methods | CustomerScoreCalculator | Identical code |
| Medium | roundTwoDecimals | CustomerScoreCalculator, CustomerScoreService | Utility duplication |
| Low | Transaction Form Submission | CustomerController | Pattern-based, but has variations |
| Low | DTO Overlapping Fields | service/dto package | Design consideration |

---

## 1. Pagination Sanitization Pattern (HIGH Priority)

### Description
The same pagination parameter sanitization logic is duplicated across multiple controller methods.

### Occurrences

**CustomerController.java:**
```java
// Lines 88-89
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 100);

// Lines 155-156
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 50);

// Lines 181-182
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 50);

// Lines 419-420
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 50);
```

**TransactionController.java:**
```java
// Lines 40-41
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 50);
```

**DebtorController.java:**
```java
// Lines 28-29
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 50);
```

**StatisticsController.java:**
```java
// Lines 67-68, 79-80, 91-92
int sanitizedPage = Math.max(page, 0);
int sanitizedSize = Math.min(Math.max(size, 1), 100);
```

### Recommendation
Create a utility class or method to handle pagination sanitization:

```java
public final class PaginationUtil {
    private PaginationUtil() {}
    
    public static int sanitizePage(int page) {
        return Math.max(page, 0);
    }
    
    public static int sanitizeSize(int size, int maxSize) {
        return Math.min(Math.max(size, 1), maxSize);
    }
    
    public static Pageable createPageable(int page, int size, int maxSize) {
        return PageRequest.of(sanitizePage(page), sanitizeSize(size, maxSize));
    }
    
    public static Pageable createPageable(int page, int size, int maxSize, Sort sort) {
        return PageRequest.of(sanitizePage(page), sanitizeSize(size, maxSize), sort);
    }
}
```

---

## 2. Form Action Methods Pattern (MEDIUM Priority)

### Description
Multiple methods in CustomerController follow the same pattern for showing action forms.

### Occurrences

**CustomerController.java (lines 306-364):**
- `showPaymentForm` (lines 306-316)
- `showSaleForm` (lines 318-328)
- `showRefundForm` (lines 330-340)
- `showFaultDiscountForm` (lines 342-352)
- `showForgivenessForm` (lines 354-364)

### Common Pattern
```java
@GetMapping("/{id}/actions/{actionName}")
public String showXxxForm(@PathVariable Long id, Model model) {
    Customer customer = customerService.get(id);
    model.addAttribute("customer", customer);
    if (!model.containsAttribute("{formName}")) {
        {FormType} form = new {FormType}();
        form.setDate(LocalDate.now());
        model.addAttribute("{formName}", form);
    }
    return "customers/actions/{actionName}";
}
```

### Recommendation
Consider creating a helper method or using a more generic approach:

```java
private <T> String showActionForm(Long id, String formName, 
                                   Supplier<T> formSupplier, 
                                   Consumer<T> formInitializer,
                                   String viewName, Model model) {
    Customer customer = customerService.get(id);
    model.addAttribute("customer", customer);
    if (!model.containsAttribute(formName)) {
        T form = formSupplier.get();
        formInitializer.accept(form);
        model.addAttribute(formName, form);
    }
    return viewName;
}
```

---

## 3. Clamp Helper Methods (HIGH Priority - Identical Code)

### Description
Two identical methods exist in the same file with different names.

### Occurrences

**CustomerScoreCalculator.java (lines 96-102):**
```java
private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
}

private static double clampRatio(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
}
```

### Recommendation
Remove `clampRatio` and use `clamp` for all cases, or if semantic distinction is desired, have `clampRatio` delegate to `clamp`:

```java
private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
}

// Remove clampRatio and replace all usages with clamp
```

---

## 4. roundTwoDecimals Duplication (MEDIUM Priority)

### Description
The same rounding utility is duplicated in two different classes.

### Occurrences

**CustomerScoreCalculator.java (lines 104-106):**
```java
private static double roundTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
}
```

**CustomerScoreService.java (lines 257-259):**
```java
private static double roundTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
}
```

### Recommendation
Move to a shared utility class:

```java
public final class MathUtil {
    private MathUtil() {}
    
    public static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
```

Or make the method in CustomerScoreCalculator package-private/public and reuse it.

---

## 5. Transaction Form Submission Pattern (LOW Priority)

### Description
POST methods for registering different transaction types follow similar patterns.

### Occurrences

**CustomerController.java:**
- `registerSale` (lines 206-217)
- `registerPayment` (lines 219-230)
- `registerRefund` (lines 232-243)
- `registerFaultDiscount` (lines 245-256)
- `forgiveDebt` (lines 258-269)
- `updateAddress` (lines 271-282)
- `updateSector` (lines 284-295)
- `updateName` (lines 382-393)

### Common Pattern
```java
@PostMapping("/{id}/{action}")
public String doAction(@PathVariable Long id,
                       @Valid @ModelAttribute("form") FormType form,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        return redirectToAction(id, redirectAttributes, "form", form, result, "actionPath");
    }
    service.doAction(id, form);
    redirectAttributes.addFlashAttribute("message", "Success message");
    return "redirect:/customers/" + id;
}
```

### Recommendation
This pattern has variations in the service method calls and success messages. While some abstraction is possible, the current implementation provides clarity. Consider keeping as-is unless the number of similar methods grows significantly.

---

## 6. DTO Overlapping Fields (LOW Priority)

### Description
Several DTOs share common fields that could potentially be consolidated through inheritance or composition.

### Occurrences

**Forms with date field:**
- `SaleForm`: date, amount, detail, itemsCount
- `PaymentForm`: date, amount
- `MoneyTransactionForm`: date, amount, detail
- `DebtForgivenessForm`: date, detail

### Recommendation
Consider creating a base class or interface:

```java
public abstract class BaseDateForm {
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    protected LocalDate date;
    
    // getter/setter
}

public class PaymentForm extends BaseDateForm {
    @NotNull
    @Min(1)
    private Integer amount;
    // ...
}
```

However, this might be over-engineering for simple DTOs. The current approach provides clear separation and is acceptable.

---

## Implementation Priority

1. **Immediate (High Priority):**
   - Remove `clampRatio` method duplication in CustomerScoreCalculator
   - Extract `roundTwoDecimals` to a shared utility

2. **Short-term (Medium Priority):**
   - Create PaginationUtil for pagination sanitization
   - Consider refactoring form action methods if more are added

3. **Long-term (Low Priority):**
   - Evaluate DTO consolidation if more forms with similar fields are added
   - Review transaction form submission patterns

---

## Notes

- The identified duplications follow common patterns in Spring MVC applications
- Some duplication is acceptable for code clarity and maintainability
- Changes should be made incrementally with proper testing
- Consider the trade-off between DRY principles and code readability
