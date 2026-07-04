import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { ToastComponent } from '../shared/toast.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  perm?: string;   // permission required; absent = always visible
}

interface NavSection {
  label: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    label: 'Core',
    items: [
      { path: '/dashboard',           label: 'Dashboard',  icon: 'dashboard'                         },
      { path: '/customers',           label: 'Customers',  icon: 'people',       perm: 'customer:read' },
      { path: '/accounts',            label: 'Accounts',   icon: 'account_circle', perm: 'account:read'  },
      { path: '/wallets',             label: 'Wallets',    icon: 'wallet',       perm: 'wallet:read'   },
      { path: '/transfers',           label: 'Transfers',  icon: 'swap_horiz',   perm: 'transfer:read' },
      { path: '/scheduled-transfers', label: 'Scheduled',  icon: 'event_repeat', perm: 'transfer:read' },
      { path: '/transactions',        label: 'Transactions', icon: 'receipt_long', perm: 'transfer:read' },
      { path: '/kyc',                 label: 'KYC',        icon: 'verified_user', perm: 'kyc:read'     },
      { path: '/audit',               label: 'Audit Trail', icon: 'history',     perm: 'audit:read'    },
    ],
  },
  {
    label: 'Finance',
    items: [
      { path: '/exchange', label: 'Exchange Rates', icon: 'currency_exchange', perm: 'wallet:read'   },
      { path: '/reports',  label: 'Reports',        icon: 'bar_chart',         perm: 'report:read'   },
    ],
  },
  {
    label: 'Admin',
    items: [
      { path: '/admin/users',    label: 'Users',    icon: 'manage_accounts', perm: 'user:assign-role' },
      { path: '/fraud',          label: 'Fraud',    icon: 'gpp_maybe',       perm: 'fraud:read'       },
      { path: '/admin/webhooks', label: 'Webhooks', icon: 'webhook',         perm: 'webhook:manage'   },
    ],
  },
];

const ALL_ITEMS: NavItem[] = NAV_SECTIONS.flatMap(s => s.items);

@Component({
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastComponent],
})
export class ShellComponent {
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);

  readonly visibleSections = computed(() => {
    const perms = this.auth.permissions();
    return NAV_SECTIONS
      .map(section => ({
        ...section,
        items: section.items.filter(item => !item.perm || perms.has(item.perm)),
      }))
      .filter(section => section.items.length > 0);
  });

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => (e as NavigationEnd).urlAfterRedirects),
      startWith(this.router.url)
    ),
    { initialValue: this.router.url }
  );

  private readonly currentItem = computed(() =>
    ALL_ITEMS.find(item => this.currentUrl().startsWith(item.path))
  );

  readonly currentTitle = computed(() => this.currentItem()?.label ?? 'Dashboard');
  readonly currentIcon  = computed(() => this.currentItem()?.icon  ?? 'dashboard');

  logout(): void { this.auth.logout(); }
}
