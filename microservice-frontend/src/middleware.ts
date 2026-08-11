import { auth } from '@/auth';

export default auth((req) => {
  const isLoggedIn = !!req.auth;
  const isAuthPage = req.nextUrl.pathname.startsWith('/login');
  const isApiAuthPage = req.nextUrl.pathname.startsWith('/api/auth');

  if (isApiAuthPage) {
    return;
  }

  if (isAuthPage) {
    if (isLoggedIn) {
      return Response.redirect(new URL('/', req.nextUrl));
    }
    return;
  }

  if (!isLoggedIn) {
    let callbackUrl = req.nextUrl.pathname;
    if (req.nextUrl.search) {
      callbackUrl += req.nextUrl.search;
    }
    return Response.redirect(
      new URL(`/login?callbackUrl=${encodeURIComponent(callbackUrl)}`, req.nextUrl)
    );
  }
});

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - api/mock (our mock API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (metadata file)
     */
    '/((?!api/mock|_next/static|_next/image|favicon.ico).*)',
  ],
};
