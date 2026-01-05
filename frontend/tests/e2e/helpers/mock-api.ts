import { Page } from '@playwright/test';

/**
 * Check if the backend is available by calling the health endpoint
 */
export async function isBackendAvailable(
  baseUrl: string = 'http://localhost:8080'
): Promise<boolean> {
  try {
    const response = await fetch(`${baseUrl}/actuator/health`, {
      method: 'GET',
      signal: AbortSignal.timeout(5000) // 5 second timeout
    });
    return response.ok;
  } catch {
    return false;
  }
}

/**
 * Setup API - either mock or real based on backend availability
 * If USE_MOCK_API env is set, always use mocks
 * If backend is available and USE_MOCK_API is not set, use real backend
 */
export async function setupApiForTest(
  page: Page,
  options: { forceMock?: boolean } = {}
): Promise<{ useMock: boolean }> {
  const forceMock = options.forceMock ?? process.env.USE_MOCK_API === 'true';

  if (forceMock) {
    await setupMockApi(page);
    return { useMock: true };
  }

  const backendAvailable = await isBackendAvailable();

  if (!backendAvailable) {
    console.log('Backend not available, using mock API');
    await setupMockApi(page);
    return { useMock: true };
  }

  console.log('Backend available, using real API');
  return { useMock: false };
}

// Mock user data
export const mockUser = {
  id: 'eng.john',
  username: 'eng.john',
  displayName: 'John C.',
  email: 'john.c@example.com',
  roles: ['USER', 'ENGINEER'],
  department: 'Engineering',
  title: 'Engineer'
};

// Mock dashboard data with proper structure
export const mockDashboard = {
  stats: {
    totalActive: 15,
    totalCompleted: 42,
    totalPending: 8,
    myTasks: 3,
    myProcesses: 5,
    pendingEscalations: 2,
    avgCompletionTimeHours: 24.5
  },
  activeByType: {
    'expense-approval': 5,
    'leave-request': 7,
    'task-assignment': 3
  },
  byStatus: {
    ACTIVE: 15,
    COMPLETED: 42,
    PENDING: 8
  },
  recentCompleted: {
    content: [],
    number: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  },
  activeProcesses: {
    content: [],
    number: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  },
  myPendingApprovals: {
    content: [],
    number: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  },
  escalationMetrics: {
    totalEscalations: 2,
    totalDeEscalations: 1,
    activeEscalatedProcesses: 1,
    escalationsByLevel: {
      MANAGER: 1,
      DIRECTOR: 1
    }
  }
};

/**
 * Setup mock API routes for testing without a backend
 */
export async function setupMockApi(page: Page) {
  // Mock getCurrentUser - return 401 for unauthenticated users
  await page.route('**/api/auth/me', async (route) => {
    const cookies = await page.context().cookies();
    const hasSession = cookies.some((c) => c.name === 'mock-session');

    if (hasSession) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockUser)
      });
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Not authenticated' })
      });
    }
  });

  // Mock login endpoint
  await page.route('**/api/auth/login', async (route) => {
    const request = route.request();
    const body = request.postDataJSON();

    if (body?.username && body?.password === 'password') {
      // Set a mock session cookie
      await page.context().addCookies([
        {
          name: 'mock-session',
          value: 'authenticated',
          domain: 'localhost',
          path: '/'
        }
      ]);

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Login successful',
          user: {
            ...mockUser,
            id: body.username,
            username: body.username
          }
        })
      });
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid credentials' })
      });
    }
  });

  // Mock logout endpoint
  await page.route('**/api/auth/logout', async (route) => {
    await page.context().clearCookies();
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Logged out' })
    });
  });

  // Mock dashboard endpoint - this is the main API for the dashboard page
  await page.route('**/api/workflow/dashboard**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockDashboard)
    });
  });

  // Mock tasks endpoints
  await page.route('**/api/tasks', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    } else {
      await route.continue();
    }
  });

  await page.route('**/api/tasks/assigned', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([])
    });
  });

  await page.route('**/api/tasks/claimable', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([])
    });
  });

  // Mock processes endpoint
  await page.route('**/api/processes', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    } else {
      await route.continue();
    }
  });

  // Mock health endpoint
  await page.route('**/health', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' })
    });
  });

  // Mock ready endpoint
  await page.route('**/ready', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' })
    });
  });

  // Mock SLA stats endpoint
  await page.route('**/api/sla/stats', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        total: 10,
        atRisk: 2,
        breached: 1,
        healthy: 7
      })
    });
  });

  // Mock analytics endpoints
  await page.route('**/api/analytics/process-duration**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { label: '0-1h', count: 5 },
        { label: '1-4h', count: 12 },
        { label: '4-8h', count: 8 },
        { label: '8-24h', count: 15 },
        { label: '24h+', count: 3 }
      ])
    });
  });

  await page.route('**/api/analytics/user-performance', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { userId: 'eng.john', tasksCompleted: 15, avgDurationHours: 2.5 },
        { userId: 'eng.sarah', tasksCompleted: 12, avgDurationHours: 3.2 }
      ])
    });
  });

  await page.route('**/api/analytics/bottlenecks', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([])
    });
  });

  // Mock notifications endpoints
  await page.route('**/api/notifications', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    } else {
      await route.continue();
    }
  });

  await page.route('**/api/notifications/unread-count', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(0)
    });
  });
}
