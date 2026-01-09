import type { Icon } from '@lucide/svelte';
import { LayoutDashboard, CheckSquare, Play, Database, FileText } from '@lucide/svelte';

export type NavigationItem = {
	title: string;
	href: string;
	icon: Icon;
	children?: NavigationItem[];
};

export const navigationSchema: NavigationItem[] = [
	{
		title: 'Dashboard',
		href: '/dashboard',
		icon: LayoutDashboard
	},
	{
		title: 'Tasks',
		href: '/tasks',
		icon: CheckSquare
	},
	{
		title: 'Processes',
		href: '/processes',
		icon: Play,
		children: [
			{
				title: 'Start Process',
				href: '/processes',
				icon: Play
			}
		]
	},
	{
		title: 'Database',
		href: '/database',
		icon: Database,
		children: [
			{
				title: 'Seed',
				href: '/database/seed',
				icon: Database
			}
		]
	}
];
