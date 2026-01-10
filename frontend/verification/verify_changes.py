from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()
    page.set_default_timeout(60000) # 60s timeout

    try:
        print("Navigating to login...")
        page.goto("http://localhost:3000/login")

        print("Filling credentials...")
        page.fill("input[type='text']", "eng.john")
        page.fill("input[type='password']", "password")
        page.click("button[type='submit']")

        print("Waiting for dashboard...")
        # Wait for either dashboard url or some dashboard element
        try:
            page.wait_for_url("**/dashboard", timeout=10000)
        except:
            print("Wait for URL failed, checking for content or forcing navigation")
            if "dashboard" not in page.url:
                 page.goto("http://localhost:3000/dashboard")

        # Verify we are somewhat logged in
        # expect(page.get_by_text("Dashboard")).to_be_visible()

        print("Navigating to Document Types...")
        page.goto("http://localhost:3000/document-types/types")
        page.wait_for_load_state("networkidle")

        # Verify Document Types page loaded
        if page.get_by_role("heading", name="Document Types").is_visible():
            print("Document Types page visible")
        else:
            print("Document Types page NOT visible")

        # Take screenshot of list
        page.screenshot(path="frontend/verification/doc_types_list.png")

        # Go to Designer
        print("Clicking Create Document Type...")
        page.click("text=+ Create Document Type")
        page.wait_for_load_state("networkidle")

        # Take screenshot of designer
        page.screenshot(path="frontend/verification/doc_type_designer.png")

        # Go to Process Manage
        print("Navigating to Process Manage...")
        page.goto("http://localhost:3000/processes/manage")
        page.wait_for_load_state("networkidle")

        # Verify new buttons exist (Compare, Suspend/Activate)
        if page.get_by_text("Compare Versions").is_visible():
             print("Compare Versions button visible")

        page.screenshot(path="frontend/verification/process_manage.png")

    except Exception as e:
        print(f"Error: {e}")
        page.screenshot(path="frontend/verification/error_state.png")

    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)
