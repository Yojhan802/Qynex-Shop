import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './base/fonts.css';
import './base/tokens.css';
import './base/reset.css';
import './base/base.css';
import './base/layout.css';
import './base/components.css';
import './base/pages.css';
import './base/responsive.css';
import './templates/storefront-base.css';
import './react-ui.css';
import './templates/index.css';
import { App } from './App';

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>);
