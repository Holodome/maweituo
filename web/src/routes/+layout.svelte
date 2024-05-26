<script lang="ts">
  import Nav from '$lib/components/Nav.svelte';
  import '../app.css';
</script>

<Nav />

<main>
  <div class="max-w-8xl mx-auto px-4 sm:px-6 md:px-8">
    <slot />
  </div>
</main>
