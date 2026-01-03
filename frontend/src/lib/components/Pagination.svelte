<script lang="ts">
  let { currentPage, totalPages, onPageChange } = $props();

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      onPageChange(page);
    }
  };
</script>

<div class="flex items-center justify-center space-x-2">
  <button
    class="px-3 py-1 border rounded"
    disabled={currentPage === 0}
    onclick={() => handlePageChange(currentPage - 1)}
  >
    Previous
  </button>
  <span>Page {currentPage + 1} of {totalPages}</span>
  <button
    class="px-3 py-1 border rounded"
    disabled={currentPage === totalPages - 1}
    onclick={() => handlePageChange(currentPage + 1)}
  >
    Next
  </button>
</div>
