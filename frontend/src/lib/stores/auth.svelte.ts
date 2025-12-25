import type { User } from '$lib/types';

// Svelte 5 runes-based auth store
class AuthStore {
	user = $state<User | null>(null);
	loading = $state(true);

	get isAuthenticated() {
		return this.user !== null;
	}

	get isUser() {
		return this.user?.roles.includes('USER') ?? false;
	}

	get isSupervisor() {
		return this.user?.roles.includes('SUPERVISOR') ?? false;
	}

	get isExecutive() {
		return this.user?.roles.includes('EXECUTIVE') ?? false;
	}

	setUser(user: User | null) {
		this.user = user;
		this.loading = false;
	}

	setLoading(loading: boolean) {
		this.loading = loading;
	}

	clear() {
		this.user = null;
		this.loading = false;
	}
}

export const authStore = new AuthStore();
