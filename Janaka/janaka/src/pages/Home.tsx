import { useEffect, useRef } from 'react';
import MasonryGrid from '../components/MasonryGrid';
import './Home.css';

const Home = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d')!;
    let animId: number;

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = 450;
    };
    resize();
    window.addEventListener('resize', resize);

    const leafColors = [
      { base: '#2d5a27', light: '#4ade80' }, // Green
      { base: '#4a7c44', light: '#86efac' }, // Moss
      { base: '#8fb339', light: '#d9f99d' }, // Lime
      { base: '#bc6c25', light: '#fde68a' }, // Autumn
      { base: '#78350f', light: '#b45309' }, // Brown
    ];

    const leaves = Array.from({ length: 50 }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      size: Math.random() * 8 + 5,
      speedY: Math.random() * 0.8 + 0.4,
      speedX: (Math.random() - 0.5) * 0.5,
      sway: Math.random() * 0.02,
      swayOffset: Math.random() * Math.PI * 2,
      
      // 3D Rotation simulation
      flip: Math.random() * Math.PI * 2,
      flipSpeed: Math.random() * 0.03 + 0.01,
      spin: Math.random() * Math.PI * 2,
      spinSpeed: (Math.random() - 0.5) * 0.04,
      
      colors: leafColors[Math.floor(Math.random() * leafColors.length)],
      opacity: Math.random() * 0.4 + 0.4,
      blur: Math.random() > 0.85 ? 1.5 : 0,
    }));

    const drawLeaf = (ctx: CanvasRenderingContext2D, l: any) => {
      ctx.save();
      ctx.translate(l.x, l.y);
      
      // Simulate 3D flipping by scaling one axis with a sine wave
      const flipScale = Math.sin(l.flip);
      ctx.rotate(l.spin);
      ctx.scale(1, flipScale);
      
      if (l.blur) ctx.filter = `blur(${l.blur}px)`;
      
      // Gradient for 3D look
      const grad = ctx.createLinearGradient(0, -l.size, 0, l.size);
      grad.addColorStop(0, l.colors.light);
      grad.addColorStop(1, l.colors.base);
      
      ctx.beginPath();
      // Pointed leaf shape
      ctx.moveTo(0, -l.size * 1.5);
      ctx.bezierCurveTo(l.size * 1.3, -l.size, l.size * 1.3, l.size, 0, l.size * 1.8);
      ctx.bezierCurveTo(-l.size * 1.3, l.size, -l.size * 1.3, -l.size, 0, -l.size * 1.5);
      
      // Subtle shadow for depth
      ctx.shadowColor = 'rgba(0,0,0,0.1)';
      ctx.shadowBlur = 4;
      ctx.shadowOffsetY = 2;
      
      ctx.fillStyle = grad;
      ctx.globalAlpha = l.opacity;
      ctx.fill();

      // Stem and Vein
      ctx.beginPath();
      ctx.moveTo(0, -l.size * 1.2);
      ctx.lineTo(0, l.size * 2.2); // Longer stem
      ctx.strokeStyle = 'rgba(0,0,0,0.1)';
      ctx.lineWidth = 0.5;
      ctx.stroke();

      ctx.restore();
    };

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      const time = Date.now() * 0.001;
      const globalWind = Math.sin(time) * 0.5; // Changing wind

      leaves.forEach(l => {
        l.y += l.speedY;
        l.x += l.speedX + globalWind + Math.sin(l.y * l.sway + l.swayOffset);
        l.flip += l.flipSpeed;
        l.spin += l.spinSpeed;
        
        if (l.y > canvas.height + 30) {
          l.y = -30;
          l.x = Math.random() * canvas.width;
          l.opacity = Math.random() * 0.4 + 0.4;
        }
        
        // Wrap around horizontally
        if (l.x > canvas.width + 30) l.x = -30;
        if (l.x < -30) l.x = canvas.width + 30;

        drawLeaf(ctx, l);
      });
      animId = requestAnimationFrame(draw);
    };
    resize();
    draw();

    return () => { cancelAnimationFrame(animId); window.removeEventListener('resize', resize); };
  }, []);

  return (
    <div className="home-container">
      <header className="hero-background">
        <canvas ref={canvasRef} className="hero-canvas" />
      </header>

      <main className="feed-section animate-fade-in">
        <h2 style={{ textAlign: 'center', margin: '20px 0', color: 'var(--text-secondary)' }}>[V2] Trending Today</h2>
        <MasonryGrid fetchUrl="/api/pins/trending" />
      </main>
    </div>
  );
};

export default Home;
