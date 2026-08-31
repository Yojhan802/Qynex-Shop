# QYNEX ECOMMERCE PLUS
# MASTER PROMPT — PROFESSIONAL MULTI-TENANT STOREFRONT TEMPLATE SYSTEM

---

## 1. ROLE

Act as a:

- Senior Frontend Engineer.
- Senior UI/UX Designer.
- Design Systems Architect.
- Ecommerce UX Specialist.
- Responsive Web Designer.
- Accessibility Specialist.
- Conversion Rate Optimization (CRO) Specialist.
- Performance Engineer.
- Visual Brand Designer.

You are working directly inside an existing production-oriented ecommerce SaaS called:

**QYNEX ECOMMERCE PLUS**

Your mission is to design and implement a complete professional storefront template system for a multi-tenant ecommerce platform.

The result must feel like a polished commercial SaaS product, not a generic template, university project, Bootstrap page, or automatically generated website.

---

# 2. PRIMARY OBJECTIVE

Build a professional visual template system containing exactly these 10 official storefront templates:

```text
CLASSIC
MINIMAL
FASHION
SPORT
LUXURY
BOUTIQUE
CATALOG
MARKET
EDITORIAL
URBAN
```

Each template must provide a genuinely different visual and interaction experience.

They must differ in:

- Layout.
- Composition.
- Visual hierarchy.
- Typography.
- Spacing.
- Cards.
- Header.
- Footer.
- Navigation.
- Hero.
- Banners.
- Product presentation.
- Category presentation.
- Catalog layout.
- Product detail layout.
- Mobile experience.
- Animations.
- Microinteractions.
- Motion language.

However, all templates must use the same existing ecommerce business logic.

---

# 3. ABSOLUTE RULE: PRESENTATION ≠ BUSINESS LOGIC

The template system controls:

**PRESENTATION**

The existing application controls:

**BUSINESS LOGIC**

Never duplicate, replace, or alter:

- Catalog logic.
- Product logic.
- Inventory.
- Stock.
- Prices.
- Discounts.
- Promotions.
- Cart.
- Checkout.
- Orders.
- Payments.
- Shipping.
- Customer authentication.
- Customer registration.
- Customer account.
- Notifications.
- API contracts.
- Tenant resolution.
- Existing business rules.

A tenant must be able to switch from one template to another without changing any commercial behavior.

Changing the template must only change how the storefront looks and behaves visually.

---

# 4. MULTI-TENANT ARCHITECTURE

The storefront is multi-tenant.

The selected template is identified through:

```text
StoreTemplate
```

Only these values are valid:

```text
CLASSIC
MINIMAL
FASHION
SPORT
LUXURY
BOUTIQUE
CATALOG
MARKET
EDITORIAL
URBAN
```

Do not invent new template identifiers.

If the backend returns:

- null
- undefined
- empty
- invalid
- unknown

use:

```text
CLASSIC
```

as the fallback.

The storefront must never become unusable because of an invalid template value.

---

# 5. CRITICAL REQUIREMENT: DO NOT CREATE 10 COLOR VARIATIONS

This is extremely important.

Do NOT implement:

> "one template + ten color palettes."

That is not the objective.

Each template must feel like a different ecommerce product.

For example:

CLASSIC should feel like a professional retail store.

FASHION should feel like a fashion editorial.

CATALOG should feel like a powerful product catalog.

LUXURY should feel exclusive.

URBAN should feel young and mobile-first.

SPORT should feel energetic.

MINIMAL should feel extremely clean.

etc.

The visual identity and interaction model must reflect the intended business type.

---

# 6. OFFICIAL TEMPLATE DEFINITIONS

---

# 6.1 CLASSIC

## Purpose

General-purpose professional ecommerce.

## Ideal for

- Retail.
- Technology.
- Electronics.
- Hardware.
- Home.
- General stores.
- Traditional businesses.
- B2C businesses.

## Visual identity

- Professional.
- Balanced.
- Trustworthy.
- Clean.
- Commercial.
- Conversion-oriented.

## Homepage

Include when real data exists:

- Professional header.
- Search bar.
- Categories.
- Hero slider.
- Featured products.
- Promotions.
- New products.
- Brands.
- Benefits.
- Footer.

## Motion

- Smooth hero carousel.
- Product carousels.
- Subtle hover.
- Soft transitions.
- Scroll reveal.

Motion must remain professional.

CLASSIC is the reference template and fallback.

---

# 6.2 MINIMAL

## Purpose

Product-first ecommerce.

## Ideal for

- Technology.
- Design products.
- Premium products.
- Lifestyle.
- Handmade products.
- Modern brands.

## Visual identity

- Large whitespace.
- Clean typography.
- Minimal borders.
- Minimal shadows.
- Large product photography.
- Strong visual hierarchy.

## Homepage

Use:

- Minimal header.
- Large hero image.
- Visual categories.
- Large product grid.
- Featured product.
- Small promotional sections.
- Minimal footer.

## Motion

Use:

- Fade.
- Subtle scale.
- Crossfade.
- Smooth image transitions.

Avoid excessive animation.

---

# 6.3 FASHION

## Purpose

Editorial fashion ecommerce.

## Ideal for

- Clothing.
- Shoes.
- Accessories.
- Fashion brands.
- Beauty.
- Lifestyle.

## Visual identity

- Editorial.
- Large photography.
- Asymmetric compositions.
- Large typography.
- Fashion magazine feeling.

## Homepage

Use:

- Full-width hero slider.
- Visual categories.
- Collections.
- Lookbook-style sections.
- Featured products.
- Editorial banners.
- Brand storytelling if real content exists.

## Product cards

Prioritize:

1. Image.
2. Product name.
3. Price.
4. Variants.
5. CTA.

Avoid unnecessary information.

## Motion

Use:

- Crossfade.
- Image transitions.
- Parallax where appropriate.
- Scroll reveal.
- Editorial transitions.

---

# 6.4 SPORT

## Purpose

Fast, energetic ecommerce.

## Ideal for

- Sportswear.
- Sneakers.
- Fitness.
- Running.
- Cycling.
- Gym equipment.
- Sports accessories.

## Visual identity

- Strong contrast.
- Bold typography.
- Fast navigation.
- Strong CTAs.
- Dynamic sections.
- Prominent promotions.

## Homepage

Use:

- Dynamic hero.
- Sport categories.
- Featured products.
- Promotions.
- Product carousels.
- Popular products.
- Brand sections.

## Motion

Motion can be faster and more energetic:

- Quick slides.
- Strong hover feedback.
- Product transitions.
- Swipe interactions.
- Immediate cart feedback.

Never compromise usability.

---

# 6.5 LUXURY

## Purpose

Premium and exclusive ecommerce.

## Ideal for

- Jewelry.
- Watches.
- Perfumes.
- Premium products.
- High-end fashion.
- Luxury home products.

## Visual identity

- Sophisticated.
- Elegant.
- Minimal.
- Large whitespace.
- Refined typography.
- Delicate borders.
- Subtle shadows.
- High-quality imagery.

## Motion

Use extremely subtle:

- Crossfade.
- Slow transitions.
- Elegant image transitions.
- Very subtle reveal animations.

Avoid:

- Excessive gradients.
- Large badges.
- Aggressive animations.
- Bright colors.
- Visual noise.

---

# 6.6 BOUTIQUE

## Purpose

Warm and human ecommerce.

## Ideal for

- Small brands.
- Boutiques.
- Handmade products.
- Cosmetics.
- Gifts.
- Crafts.
- Independent brands.

## Visual identity

- Warm.
- Friendly.
- Personal.
- Soft.
- Brand-oriented.

## Homepage

Use:

- Warm hero.
- Visual categories.
- Featured products.
- Storytelling sections.
- Brand story when real data exists.
- Promotions.
- Testimonials only if supported by the system.

## Motion

- Soft reveal.
- Gentle image transitions.
- Horizontal product carousels.
- Natural microinteractions.

---

# 6.7 CATALOG

## Purpose

Maximum product discovery efficiency.

## Ideal for

- Hardware.
- Spare parts.
- Electronics.
- Distributors.
- Wholesale.
- Large catalogs.

## Visual identity

- Dense.
- Efficient.
- Structured.
- Information-focused.

## Homepage

Use:

- Search-first header.
- Categories.
- Featured products.
- Product sections.
- Promotions.

## Catalog

This template must prioritize:

- Filters.
- Search.
- Sorting.
- Product density.
- Category navigation.
- Price.
- Stock.
- Variants.

Desktop should use available horizontal space efficiently.

Mobile should transform filters into:

```text
[FILTERS] [SORT]
```

using a drawer or bottom sheet when appropriate.

## Motion

Minimal.

Motion should improve usability, not decoration.

---

# 6.8 MARKET

## Purpose

Large multi-category store.

## Ideal for

- General marketplaces.
- Department-style stores.
- Large retail catalogs.
- Multi-category businesses.

## Visual identity

- Rich.
- Dynamic.
- Organized.
- Discoverable.

## Homepage

Use:

- Full header.
- Search.
- Category navigation.
- Hero carousel.
- Featured categories.
- Multiple product carousels.
- Offers.
- New arrivals.
- Brands.
- Category sections.
- Benefits.

The page should feel rich without becoming chaotic.

## Motion

Use:

- Carousels.
- Horizontal category scrolling.
- Product sliders.
- Smooth transitions.
- Scroll reveal.

---

# 6.9 EDITORIAL

## Purpose

Storytelling + ecommerce.

## Ideal for

- Fashion.
- Lifestyle.
- Beauty.
- Decoration.
- Premium brands.
- Brands with strong identity.

## Visual identity

The store should feel like a digital magazine.

Use:

- Large typography.
- Editorial layouts.
- Asymmetric sections.
- Large images.
- Storytelling.
- Collections.
- Product features.

## Motion

Use:

- Scroll reveal.
- Crossfade.
- Subtle parallax.
- Editorial transitions.
- Image reveal.

The product must remain purchasable and accessible.

---

# 6.10 URBAN

## Purpose

Young, modern, mobile-first ecommerce.

## Ideal for

- Streetwear.
- Sneakers.
- Accessories.
- Youth fashion.
- Technology.
- Urban culture.

## Visual identity

- Modern.
- Bold.
- Young.
- Fast.
- Mobile-first.

## Homepage

Use:

- Full-width hero.
- Horizontal category navigation.
- Product carousels.
- Promotions.
- Featured products.
- Brand sections.

## Motion

- Swipe.
- Fast transitions.
- Product card interaction.
- Horizontal scrolling.
- Sticky CTAs.
- Mobile-focused microinteractions.

---

# 7. REQUIRED PAGES

Every template must fully support:

```text
tienda/index.html
tienda/producto.html
tienda/carrito.html
tienda/checkout.html
tienda/cuenta/login.html
tienda/cuenta/registro.html
tienda/cuenta/pedidos.html
tienda/no-disponible.html
```

Do not only redesign the homepage.

The entire purchasing journey must maintain the visual identity of the selected template.

---

# 8. HEADER SYSTEM

Every template must have its own header composition.

Do not simply reuse the exact same header with different colors.

Headers must support, when available:

- Company logo.
- Company name.
- Search.
- Categories.
- Account.
- Cart.
- Cart quantity.
- Navigation.
- Mobile menu.

Possible visual strategies:

CLASSIC:

```text
Logo | Categories | Search | Account | Cart
```

MINIMAL:

```text
Logo | Navigation | Search | Account | Cart
```

FASHION:

```text
Logo | Collections | Categories | Search | Account | Cart
```

CATALOG:

```text
Logo | Categories | Large Search | Account | Cart
```

MARKET:

```text
Logo | Categories | Search | Account | Cart
```

etc.

The exact composition may vary.

Functionality may not.

---

# 9. HERO SYSTEM

When real banners exist, support:

- Hero slider.
- Multiple slides.
- Images.
- Headline.
- Description.
- CTA.
- Promotional information.
- Indicators.
- Navigation arrows.

Support:

- Swipe.
- Touch.
- Keyboard navigation.
- Responsive layouts.

Autoplay is allowed only when it improves the experience.

Pause when appropriate.

Do not create fake promotional content.

If there are no real banners:

**hide the hero elegantly or use a suitable data-driven alternative.**

Never show:

```text
Lorem ipsum
Demo Product
50% OFF
Buy now
```

unless the data actually exists.

---

# 10. DYNAMIC CONTENT

When supported by the existing backend, templates must be capable of displaying:

- Categories.
- Subcategories.
- Brands.
- Featured products.
- New products.
- Discounted products.
- Best sellers.
- Collections.
- Promotions.
- Banners.
- Benefits.
- Customer information.
- AI assistant.

If a section has no data:

**do not leave a large empty space.**

Hide the section gracefully.

---

# 11. PRODUCT CARD SYSTEM

Create a professional reusable product card.

Support:

- Product image.
- Secondary image when available.
- Product name.
- Brand.
- Price.
- Previous price.
- Discount.
- Stock.
- Badge.
- Variant information.
- Color.
- Size.
- CTA.

States:

```text
normal
hover
focus
loading
disabled
out-of-stock
discount
new
```

Only show states supported by actual data.

---

# 12. PRODUCT CARD INTERACTIONS

On desktop hover, when appropriate:

- Slight elevation.
- Secondary image.
- Subtle zoom.
- CTA appearance.
- Information transition.

On mobile:

Do not depend on hover.

Interactions must work through touch.

When adding to cart:

Provide immediate visual feedback.

Example:

```text
ADD TO CART

↓

✓ ADDED
```

If technically possible without modifying business logic, a subtle visual movement toward the cart may be used.

Never create another cart implementation.

---

# 13. PRODUCT DETAIL PAGE

This page must receive high design priority.

Support:

- Image gallery.
- Main image.
- Thumbnails.
- Swipe.
- Zoom if already supported.
- Fullscreen/lightbox if already supported.
- Product name.
- Brand.
- Price.
- Previous price.
- Discount.
- Stock.
- Variants.
- Colors.
- Sizes.
- Attributes.
- Quantity.
- Add to cart.
- Buy now.
- Shipping information.
- Payment information.
- Description.
- Features.
- Additional information.

Primary objective:

**make purchasing extremely clear.**

---

# 14. PRODUCT GALLERY

Use an interactive gallery.

Desktop:

```text
THUMBNAILS | MAIN IMAGE | PRODUCT INFORMATION
```

Mobile:

```text
← IMAGE → 
1 / 5
```

Support when appropriate:

- Swipe.
- Thumbnails.
- Zoom.
- Fullscreen.
- Keyboard navigation.

---

# 15. STICKY PRODUCT PURCHASE

When appropriate, the product purchase panel may remain visible during scrolling.

On mobile, when appropriate:

```text
┌─────────────────────────────┐
│ S/ XXX.XX       [BUY NOW]   │
└─────────────────────────────┘
```

The sticky CTA must:

- Not cover important content.
- Respect safe areas.
- Remain accessible.
- Not duplicate business logic.

---

# 16. CATALOG PAGE

The catalog must support the existing:

- Search.
- Categories.
- Filters.
- Brands.
- Price.
- Availability.
- Sorting.
- Pagination/load-more if already supported.

Desktop:

Use a structured grid.

Mobile:

Use:

```text
[FILTERS] [SORT]
```

Filters may appear as:

- Drawer.
- Bottom sheet.
- Modal.

Use whichever best matches the template.

---

# 17. HORIZONTAL SCROLL

Use horizontal scrolling where it improves mobile UX.

Especially for:

- Categories.
- Product carousels.
- Brands.
- Collections.
- Promotions.

Example:

```text
← [CATEGORY] [CATEGORY] [CATEGORY] →
```

Do not force every section into a vertical list.

The interaction should feel natural on touch devices.

---

# 18. PRODUCT CAROUSELS

Where appropriate, use:

```text
← Product | Product | Product | Product →
```

Desktop:

Show multiple products.

Tablet:

Reduce visible products.

Mobile:

Use swipeable cards.

Carousels must:

- Work with touch.
- Work with mouse.
- Support keyboard navigation.
- Have accessible labels.
- Not trap focus.
- Not cause horizontal overflow outside the intended container.

---

# 19. CAROUSEL TYPES

Use different carousel styles depending on the template.

Examples:

CLASSIC:

Product carousel.

MINIMAL:

Elegant horizontal product slider.

FASHION:

Editorial image slider.

SPORT:

Fast promotional slider.

LUXURY:

Slow image crossfade.

BOUTIQUE:

Warm collection carousel.

CATALOG:

Functional product slider.

MARKET:

Multiple category/product carousels.

EDITORIAL:

Large storytelling slider.

URBAN:

Mobile swipe carousel.

---

# 20. STICKY HEADER

When appropriate, the header may become compact after scrolling.

Example:

Initial:

```text
LOGO | CATEGORIES | SEARCH | ACCOUNT | CART
```

Scrolled:

```text
LOGO | SEARCH | ACCOUNT | CART
```

Use smooth transitions.

Do not hide critical functionality.

---

# 21. SCROLL REVEAL

Sections may animate into view using:

- Fade.
- Translate.
- Scale.

Do not animate everything.

Avoid:

- Excessive movement.
- Delayed content.
- Layout shifts.
- Scroll hijacking.

The page must remain usable even if animations fail.

---

# 22. PARALLAX

Parallax may be used only where it improves the visual composition.

Good candidates:

- Hero.
- Editorial banners.
- Large visual sections.

Parallax must be:

- Subtle.
- Lightweight.
- Optional for reduced-motion users.

Never make the entire page move excessively.

---

# 23. FILTER DRAWERS AND BOTTOM SHEETS

Mobile filters should feel native and modern.

Example:

```text
┌──────────────────────────┐
│ Filters              X   │
│                          │
│ Categories               │
│ Brands                   │
│ Price                    │
│ Availability              │
│                          │
│ [ APPLY FILTERS ]        │
└──────────────────────────┘
```

Must support:

- Keyboard accessibility.
- Escape.
- Focus management.
- Touch.
- Scroll.
- Clear close action.

Use existing filter logic.

---

# 24. CART PAGE

The cart must clearly display:

- Products.
- Image.
- Name.
- Variant.
- Price.
- Quantity.
- Subtotal.
- Remove action.
- Discounts when applicable.
- Shipping when applicable.
- Final total.
- Checkout CTA.

---

# 25. EMPTY CART

Do not simply display:

```text
Your cart is empty.
```

Create a polished empty state with:

- Visual.
- Message.
- Helpful explanation.
- CTA to continue shopping.

Do not invent functionality.

---

# 26. CHECKOUT

The checkout must prioritize:

**clarity + trust + speed + conversion.**

Support the existing flow for:

- Customer information.
- Address.
- Shipping.
- Payment.
- Payment instructions.
- Order summary.
- Confirmation.
- Errors.
- Loading.

Do not redesign the business logic.

Only redesign the presentation.

Do not create a second checkout.

---

# 27. CHECKOUT VISUAL PRIORITY

The checkout should contain minimal distractions.

Do not include unnecessary:

- Banners.
- Marketing animations.
- Large promotional sections.
- Navigation elements that distract from completing the purchase.

The customer must always understand:

```text
WHAT AM I BUYING?
HOW MUCH AM I PAYING?
HOW WILL I PAY?
WHERE WILL IT BE DELIVERED?
HOW DO I CONFIRM?
```

---

# 28. LOGIN

Design:

- Email/username.
- Password.
- Show/hide password.
- Remember option if existing.
- Login button.
- Recovery option if existing.
- Error state.
- Loading state.

Never add unsupported fields.

---

# 29. REGISTRATION

Use existing backend fields.

Support:

- Validation.
- Password.
- Confirmation.
- Errors.
- Loading.
- Success.

Do not invent additional customer data requirements.

---

# 30. CUSTOMER ACCOUNT

For:

```text
tienda/cuenta/pedidos.html
```

Design:

- Order list.
- Order number.
- Date.
- Total.
- Status.
- Details.
- Delivery status if available.
- Payment receipt if available.

Support:

```text
loading
empty
error
success
```

---

# 31. STORE UNAVAILABLE

Create a professional page for:

```text
tienda/no-disponible.html
```

It may represent:

- Suspended store.
- Temporarily closed store.
- Unavailable store.
- Tenant resolution error.

Never expose raw technical errors.

---

# 32. GLOBAL STATES

Every important component must support:

### Loading

Use skeletons when appropriate.

### Empty

Create useful empty states.

### Error

Explain the problem clearly.

### Success

Provide feedback.

### Disabled

Make disabled states visually obvious.

### Out of stock

Make availability clear.

### Suspended

Use professional messaging.

---

# 33. SKELETON LOADING

Use skeleton loaders where appropriate.

Skeletons must match the real component structure.

Example:

```text
┌───────────────┐
│               │
│    IMAGE      │
│               │
├───────────────┤
│ ███████████   │
│ ███████       │
│ █████         │
└───────────────┘
```

Do not create giant generic loading screens when localized loading is possible.

---

# 34. DESIGN SYSTEM

Create reusable design tokens.

## Colors

```css
--color-primary
--color-secondary
--color-accent
--color-background
--color-surface
--color-surface-alt
--color-text
--color-text-muted
--color-border
--color-success
--color-warning
--color-error
```

## Typography

```css
--font-heading
--font-body
--font-size-xs
--font-size-sm
--font-size-md
--font-size-lg
--font-size-xl
--font-size-2xl
--font-size-3xl
--font-size-4xl
```

## Spacing

```css
--space-1
--space-2
--space-3
--space-4
--space-5
--space-6
--space-8
--space-10
--space-12
--space-16
```

## Radius

```css
--radius-sm
--radius-md
--radius-lg
--radius-xl
--radius-full
```

## Shadows

```css
--shadow-sm
--shadow-md
--shadow-lg
```

---

# 35. TENANT CUSTOMIZATION

The tenant may customize visual properties.

Support existing configuration for:

- Logo.
- Favicon.
- Primary color.
- Secondary/accent color.
- Approved fonts.
- Background.
- Light/dark mode if supported.
- Component radius.
- Hero/banner.
- Promotional images.
- Welcome text.
- Category visibility.
- Brand visibility.
- Promotion visibility.
- AI assistant visibility.

Customization must work through controlled design variables.

---

# 36. SECURITY

Never allow tenants to inject:

```text
Arbitrary CSS
Arbitrary JavaScript
Arbitrary HTML
Arbitrary iframes
Arbitrary scripts
Inline event handlers
```

Never execute tenant-provided code.

All dynamic content must be treated as data.

Escape dynamic text before inserting it into HTML.

Do not expose:

- API secrets.
- Tokens.
- Credentials.
- Internal identifiers that should remain private.

---

# 37. DYNAMIC DATA

Never hardcode:

- Company names.
- Products.
- Prices.
- Categories.
- Brands.
- Stock.
- Discounts.
- Promotions.
- Payment methods.
- Shipping rules.
- Orders.
- API URLs.
- Credentials.

All content must come from the existing system.

---

# 38. EXISTING API ARCHITECTURE

Before modifying anything, inspect the existing implementation completely.

At minimum inspect:

```text
front/tienda/
front/tienda/js/store/
store-shell.js
store-api.js
checkout
cart
products
orders
authentication
tenant configuration
```

Identify:

- APIs.
- Functions.
- Events.
- DOM selectors.
- State management.
- Persistence.
- Tenant resolution.
- Cart implementation.
- Checkout implementation.
- Authentication.
- Orders.
- Notifications.
- Configuration.

Do not assume how the system works.

Read the code first.

---

# 39. REUSE EXISTING LOGIC

If the system already contains:

```javascript
addToCart()
```

do not create:

```javascript
addProductToNewCart()
```

If it already contains:

```javascript
checkout()
```

do not create a second checkout system.

If it already contains:

```javascript
getProducts()
```

do not create another product API.

The template must consume existing functionality.

---

# 40. COMPONENT ARCHITECTURE

Where compatible with the existing architecture, create reusable components such as:

```text
Header
MobileHeader
Navigation
Search
CategoryNavigation
Hero
Banner
Section
ProductCard
ProductGrid
ProductCarousel
ProductGallery
ProductInfo
VariantSelector
QuantitySelector
Price
DiscountBadge
StockBadge
CartItem
CartSummary
CheckoutSummary
PaymentMethod
LoginForm
RegisterForm
OrderCard
EmptyState
ErrorState
LoadingState
Footer
AIWidget
```

Adapt names to the project's existing architecture.

Do not create duplicate implementations unnecessarily.

---

# 41. RESPONSIVE DESIGN

Design explicitly for:

```text
320px
360px
375px
390px
414px
480px
768px
1024px
1280px
1440px
1920px+
```

Do not consider responsive design complete merely by adding one media query.

Verify:

- Layout.
- Typography.
- Navigation.
- Product cards.
- Product gallery.
- Carousels.
- Filters.
- Cart.
- Checkout.
- Modals.
- Drawers.
- Sticky elements.

---

# 42. MOBILE-FIRST UX

Mobile is not a reduced desktop version.

Design mobile as a first-class experience.

Use:

- Compact header.
- Touch-friendly controls.
- Horizontal category scrolling.
- Swipeable product carousels.
- Mobile filter drawers.
- Bottom sheets when appropriate.
- Sticky purchase CTA when useful.
- Optimized product gallery.
- Simplified checkout presentation.

Minimum touch target:

```text
44px × 44px
```

---

# 43. ACCESSIBILITY

Target:

**WCAG 2.2 AA**

Implement:

- Sufficient contrast.
- Keyboard navigation.
- Visible focus.
- Accessible forms.
- Labels.
- Alt text.
- Semantic HTML.
- Accessible menus.
- Accessible carousels.
- Accessible dialogs.
- Accessible error messages.
- Reduced-motion support.

Do not use color as the only indication of status.

---

# 44. MOTION DESIGN

The storefront must feel alive and modern.

Use motion for:

- Carousels.
- Sliders.
- Product hover.
- Product image changes.
- Cart feedback.
- Scroll reveal.
- Sticky transitions.
- Filters.
- Drawers.
- Modals.
- State changes.
- Gallery transitions.

But motion must always have a purpose.

Ask:

> Does this animation improve understanding, navigation, discovery, or conversion?

If not:

**do not add it.**

---

# 45. DIFFERENT MOTION LANGUAGE PER TEMPLATE

Do not use the exact same animation system everywhere.

## CLASSIC

Smooth and professional.

## MINIMAL

Subtle fades and elegant transitions.

## FASHION

Editorial movement and image transitions.

## SPORT

Fast and energetic.

## LUXURY

Slow, subtle and sophisticated.

## BOUTIQUE

Warm and natural.

## CATALOG

Minimal and productivity-focused.

## MARKET

Dynamic discovery-oriented motion.

## EDITORIAL

Storytelling transitions and subtle parallax.

## URBAN

Fast mobile-first interactions.

---

# 46. PERFORMANCE

Optimize:

- Images.
- Responsive images.
- Lazy loading.
- CSS.
- JavaScript.
- Fonts.
- Layout shifts.
- Initial rendering.
- Animations.

Prefer animation using:

```text
transform
opacity
```

Avoid unnecessary animation of layout properties.

Do not introduce heavy dependencies without a strong reason.

Do not make external services mandatory for the storefront to work.

The store must feel fast on mobile devices and modest connections.

---

# 47. IMAGE PERFORMANCE

Use when appropriate:

```text
loading="lazy"
aspect-ratio
object-fit
responsive images
```

Prevent layout shifts.

Never stretch product images.

Preserve correct aspect ratios.

---

# 48. REDUCED MOTION

Must support:

```css
@media (prefers-reduced-motion: reduce)
```

When enabled:

- Reduce transitions.
- Disable parallax.
- Reduce autoplay.
- Remove unnecessary movement.

All functionality must continue working.

---

# 49. FOOTER

Create a professional footer matching each template.

When real data exists, it may contain:

- Company.
- Categories.
- Contact.
- Social media.
- Policies.
- Terms.
- Payment methods.
- Shipping information.
- Copyright.

Never invent information.

If a data field does not exist:

**hide it.**

---

# 50. SEARCH UX

Use the existing search functionality.

Design states for:

- Default.
- Focus.
- Loading.
- Results.
- No results.
- Error.

If the backend supports product/category suggestions, present them elegantly.

Do not build a new search engine.

---

# 51. NO FAKE FEATURES

Do NOT invent:

- Wishlist.
- Reviews.
- Product comparison.
- Loyalty.
- Coupons.
- Tracking.
- Chat.
- AI recommendations.
- Favorites.

unless those features already exist in the system.

If a feature exists:

**design it professionally.**

If it does not exist:

**do not create it only for visual purposes.**

---

# 52. DATA-DRIVEN SECTIONS

Every section must be conditional on real data.

If:

```text
featuredProducts = []
```

do not render an empty featured section.

If:

```text
brands = []
```

do not render an empty brand carousel.

If there are no banners:

do not render an empty hero.

The UI must adapt naturally to the actual tenant data.

---

# 53. VISUAL QUALITY STANDARD

The result must NOT look like:

- A university project.
- A generic Bootstrap template.
- A basic Tailwind template.
- A dashboard converted into ecommerce.
- A landing page pretending to be ecommerce.
- Ten copies of the same page.
- AI-generated generic UI.

The result must feel like:

**a premium commercial ecommerce SaaS storefront system.**

Think in terms of:

- Strong visual hierarchy.
- Professional typography.
- High-quality spacing.
- Excellent image treatment.
- Conversion-focused UX.
- Modern motion.
- Responsive behavior.
- Consistency.
- Accessibility.
- Performance.

---

# 54. TEMPLATE DIFFERENTIATION MATRIX

Use this matrix as a mandatory design direction:

| Template | Primary Goal | Visual Style | Motion | Mobile Priority |
|---|---|---|---|---|
| CLASSIC | Trust + Conversion | Professional | Smooth | High |
| MINIMAL | Product Focus | Clean | Subtle | High |
| FASHION | Brand + Image | Editorial | Rich | High |
| SPORT | Fast Conversion | Energetic | Fast | Very High |
| LUXURY | Exclusivity | Sophisticated | Elegant | High |
| BOUTIQUE | Brand + Connection | Warm | Soft | High |
| CATALOG | Product Discovery | Dense | Minimal | Very High |
| MARKET | Discovery | Rich | Dynamic | Very High |
| EDITORIAL | Storytelling | Narrative | Editorial | High |
| URBAN | Mobile + Speed | Modern | Fast | Extremely High |

---

# 55. IMPLEMENTATION PHASES

Follow these phases in order.

---

## PHASE 1 — COMPLETE AUDIT

Before writing implementation code:

Inspect:

```text
front/tienda/
front/tienda/js/store/
store-shell.js
store-api.js
```

and all related pages/components.

Understand:

- Architecture.
- APIs.
- State.
- Tenant resolution.
- Cart.
- Checkout.
- Authentication.
- Orders.
- Payments.
- Existing styles.
- Existing configuration.

Do not make destructive changes.

---

## PHASE 2 — VISUAL ARCHITECTURE

Define:

- Template architecture.
- Design tokens.
- Component structure.
- Responsive strategy.
- Motion system.
- Data-driven sections.
- Template switching mechanism.

---

## PHASE 3 — DESIGN TOKENS

Create controlled variables for:

- Colors.
- Typography.
- Spacing.
- Radius.
- Shadows.
- Transitions.
- Container widths.
- Grid gaps.
- Product card style.

---

## PHASE 4 — BASE COMPONENT SYSTEM

Create/reuse components required by all templates.

Avoid unnecessary duplication.

---

## PHASE 5 — CLASSIC

Implement CLASSIC first.

CLASSIC must become:

**the stable reference implementation and fallback.**

Verify all functionality before continuing.

---

## PHASE 6 — REMAINING TEMPLATES

Implement:

```text
MINIMAL
FASHION
SPORT
LUXURY
BOUTIQUE
CATALOG
MARKET
EDITORIAL
URBAN
```

Each must have a clearly recognizable identity.

---

## PHASE 7 — RESPONSIVE QA

Test all templates across:

```text
320
375
390
414
768
1024
1280
1440
1920
```

---

## PHASE 8 — FUNCTIONAL QA

Test:

### Catalog

- Products.
- Categories.
- Brands.
- Search.
- Filters.
- Sorting.

### Product

- Variants.
- Quantity.
- Stock.
- Price.
- Add to cart.

### Cart

- Add.
- Remove.
- Quantity.
- Persistence.
- Totals.

### Checkout

- Customer.
- Shipping.
- Payment.
- Confirmation.
- Errors.

### Account

- Login.
- Registration.
- Session.
- Orders.

---

# 56. MULTI-TENANT TESTING

Verify:

```text
Tenant A ≠ Tenant B
```

Never allow data leakage between tenants.

Verify that changing templates does not change:

- Product data.
- Orders.
- Cart.
- Configuration.
- Customer data.
- Payment configuration.

---

# 57. TEMPLATE SWITCH TEST

Test:

```text
CLASSIC
↓
MINIMAL
↓
FASHION
↓
SPORT
↓
LUXURY
↓
BOUTIQUE
↓
CATALOG
↓
MARKET
↓
EDITORIAL
↓
URBAN
```

The same tenant data must remain valid.

Only presentation should change.

---

# 58. REGRESSION TESTING

After implementing templates, verify:

- Existing APIs still work.
- Existing checkout still works.
- Existing cart still works.
- Existing payments still work.
- Existing authentication still works.
- Existing orders still work.
- Existing tenant resolution still works.

Do not modify tests to hide failures.

Fix the underlying issue.

---

# 59. CODE QUALITY

Maintain:

- Clean architecture.
- Reusable components.
- Clear naming.
- Small functions.
- Minimal duplication.
- No dead code.
- No unnecessary dependencies.
- No inline business logic inside visual components when avoidable.

Do not introduce unnecessary frameworks or libraries.

Respect the existing project architecture.

---

# 60. VALIDATION COMMANDS

Run the tools available in the project.

Where applicable:

```bash
node --check
```

Also run:

```text
Build
Lint
Typecheck
Existing tests
```

Fix all errors caused by your implementation.

---

# 61. DELIVERABLES PER TEMPLATE

For each template provide:

## Identity

- Template name.
- Identifier.
- Purpose.
- Ideal business type.
- Visual philosophy.

## Design System

- Colors.
- Typography.
- Spacing.
- Radius.
- Shadows.
- Buttons.
- Inputs.
- Cards.

## Homepage

- Header.
- Hero.
- Categories.
- Product sections.
- Promotions.
- Banners.
- Footer.

## Catalog

- Search.
- Filters.
- Sorting.
- Product grid.
- Pagination/load-more if existing.

## Product

- Gallery.
- Product information.
- Variants.
- Stock.
- Pricing.
- CTA.

## Cart

- Product items.
- Summary.
- Empty state.
- Loading.
- Errors.

## Checkout

- Customer.
- Shipping.
- Payment.
- Summary.
- Confirmation.
- Errors.

## Account

- Login.
- Registration.
- Orders.

## Dynamic states

- Loading.
- Empty.
- Error.
- Success.
- Disabled.
- Out-of-stock.
- Suspended.

## Responsive

Verify:

```text
320px
768px
1440px
```

at minimum.

---

# 62. ACCESSIBILITY CHECKLIST

For every template:

- [ ] WCAG 2.2 AA.
- [ ] Keyboard navigation.
- [ ] Visible focus.
- [ ] Accessible forms.
- [ ] Semantic HTML.
- [ ] Alt text.
- [ ] Accessible dialogs.
- [ ] Accessible carousels.
- [ ] Accessible errors.
- [ ] Reduced motion.
- [ ] Touch targets ≥44px.
- [ ] Contrast checked.

---

# 63. PERFORMANCE CHECKLIST

For every template:

- [ ] Responsive images.
- [ ] Lazy loading.
- [ ] No unnecessary dependencies.
- [ ] No excessive JavaScript.
- [ ] No layout shifts.
- [ ] Lightweight animations.
- [ ] Fast initial render.
- [ ] Mobile optimized.
- [ ] No mandatory external services.

---

# 64. FINAL ACCEPTANCE CRITERIA

The implementation is considered complete only when:

### Template system

- [ ] All 10 templates exist.
- [ ] Only official identifiers are used.
- [ ] CLASSIC works as fallback.
- [ ] Each template has a distinct identity.
- [ ] Templates are not simple color variations.

### Store

- [ ] Tenant selects the template.
- [ ] Correct template loads publicly.
- [ ] Real catalog works.
- [ ] Real products work.
- [ ] Cart works.
- [ ] Cart persists.
- [ ] Checkout works.
- [ ] Payments work.
- [ ] Orders work.
- [ ] Authentication works.
- [ ] Customer account works.

### Multi-tenancy

- [ ] Tenant isolation works.
- [ ] No cross-tenant data leakage.
- [ ] Branding is tenant-specific.
- [ ] Template selection is tenant-specific.

### UX

- [ ] Responsive.
- [ ] Mobile-first.
- [ ] Carousels work.
- [ ] Swipe works.
- [ ] Filters work.
- [ ] Product gallery works.
- [ ] Sticky elements work correctly.
- [ ] Empty states work.
- [ ] Error states work.
- [ ] Loading states work.

### Technical

- [ ] No business logic duplicated.
- [ ] No checkout duplicated.
- [ ] No cart duplicated.
- [ ] No secrets exposed.
- [ ] No arbitrary tenant code execution.
- [ ] Existing APIs preserved.
- [ ] Build passes.
- [ ] Tests pass.
- [ ] Lint passes when available.
- [ ] Typecheck passes when available.

---

# 65. CRITICAL RULES

NEVER:

- Invent data.
- Invent APIs.
- Invent business logic.
- Duplicate the cart.
- Duplicate checkout.
- Modify payment logic.
- Modify stock logic.
- Modify order logic.
- Expose secrets.
- Allow arbitrary CSS/JS/HTML.
- Break multi-tenancy.
- Create fake features.
- Use placeholder content in production UI.
- Create ten copies of the same template.
- Sacrifice performance for animation.
- Sacrifice accessibility for aesthetics.

---

# 66. FINAL DESIGN PRINCIPLE

The goal is NOT:

> "Make the existing ecommerce prettier."

The goal is:

> **Build a complete professional storefront design system capable of presenting the same ecommerce engine through 10 distinct commercial experiences.**

The customer should be able to select:

```text
CLASSIC
MINIMAL
FASHION
SPORT
LUXURY
BOUTIQUE
CATALOG
MARKET
EDITORIAL
URBAN
```

and immediately feel that the entire store has changed.

At the same time:

```text
Products
↓
Cart
↓
Checkout
↓
Payment
↓
Order
↓
Customer Account
```

must continue using exactly the same underlying business logic.

---

# 67. FINAL INSTRUCTION TO THE AGENT

Do not immediately start modifying files.

First inspect the entire relevant storefront architecture.

Understand the existing implementation.

Then define the visual architecture and design tokens.

Then implement CLASSIC.

Verify that CLASSIC works completely.

Only then implement the remaining templates.

Every template must be:

**professional, responsive, dynamic, interactive, accessible, performant and visually distinct.**

Use intelligently:

```text
Carousels
Sliders
Swipe
Horizontal scrolling
Sticky headers
Sticky CTAs
Product galleries
Scroll reveal
Microinteractions
Hover states
Skeleton loading
Drawers
Bottom sheets
Modals
Crossfade
Parallax
Transitions
```

but only when they improve UX.

The storefront should feel alive.

It should not feel static.

It should not feel overloaded.

It should not feel like a generic template.

It should feel like a **premium ecommerce SaaS product ready to be used by real businesses.**

If a visual requirement conflicts with existing business logic or architecture:

**DO NOT BREAK THE SYSTEM.**

Stop at that point, explain the conflict, identify the affected files/functions, and propose the minimum safe architectural change before implementing it.

**Business logic is authoritative.  
The template is presentation.  
The existing ecommerce engine must remain the source of truth.**