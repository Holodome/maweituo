<script>
  import Nav from './Nav.svelte';
  import '../app.css';
</script>

<Nav />

<main class="container">
  <div class="max-w-8xl mx-auto px-4 sm:px-6 md:px-8">
    <slot />
  </div>
</main>
